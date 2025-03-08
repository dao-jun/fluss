/*
 * Copyright (c) 2025 Alibaba Group Holding Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.fluss.kafka;

import com.alibaba.fluss.shaded.netty4.io.netty.buffer.ByteBuf;
import com.alibaba.fluss.shaded.netty4.io.netty.channel.ChannelHandlerContext;
import com.alibaba.fluss.shaded.netty4.io.netty.util.ReferenceCountUtil;

import org.apache.kafka.common.protocol.ApiKeys;
import org.apache.kafka.common.protocol.ApiMessage;
import org.apache.kafka.common.protocol.ByteBufferAccessor;
import org.apache.kafka.common.protocol.ObjectSerializationCache;
import org.apache.kafka.common.requests.AbstractRequest;
import org.apache.kafka.common.requests.AbstractResponse;
import org.apache.kafka.common.requests.RequestHeader;
import org.apache.kafka.common.requests.ResponseHeader;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

public class KafkaRequest {
    private static final AtomicLong ID_GENERATOR = new AtomicLong(0);

    private final ApiKeys apiKey;
    private final short apiVersion;
    private final long requestId = ID_GENERATOR.getAndIncrement();
    private final RequestHeader header;
    private final AbstractRequest request;
    private final ByteBuf buffer;
    private final ChannelHandlerContext ctx;
    private final long startTimeMs;
    private final CompletableFuture<AbstractResponse> future;
    private volatile boolean cancelled = false;

    protected KafkaRequest(
            ApiKeys apiKey,
            short apiVersion,
            RequestHeader header,
            AbstractRequest request,
            ByteBuf buffer,
            ChannelHandlerContext ctx,
            CompletableFuture<AbstractResponse> future) {
        this.apiKey = apiKey;
        this.apiVersion = apiVersion;
        this.header = header;
        this.request = request;
        this.buffer = buffer.retain();
        this.ctx = ctx;
        this.startTimeMs = System.currentTimeMillis();
        this.future = future;
    }

    public ApiKeys apiKey() {
        return apiKey;
    }

    public short apiVersion() {
        return apiVersion;
    }

    public long requestId() {
        return requestId;
    }

    public RequestHeader header() {
        return header;
    }

    public <T> T request() {
        return (T) request;
    }

    public void releaseBuffer() {
        ReferenceCountUtil.safeRelease(buffer);
    }

    public ChannelHandlerContext ctx() {
        return ctx;
    }

    public long startTimeMs() {
        return startTimeMs;
    }

    public CompletableFuture<AbstractResponse> future() {
        return future;
    }

    public void complete(AbstractResponse response) {
        future.complete(response);
    }

    public void fail(Throwable t) {
        future.completeExceptionally(t);
    }

    public void cancel() {
        cancelled = true;
    }

    public boolean cancelled() {
        return cancelled;
    }

    public ByteBuf serialize() {
        try {
            AbstractResponse response = future.join();
            return serialize(response);
        } catch (Throwable t) {
            AbstractResponse response = request.getErrorResponse(t);
            return serialize(response);
        } finally {
            releaseBuffer();
        }
    }

    private ByteBuf serialize(AbstractResponse response) {
        final ObjectSerializationCache cache = new ObjectSerializationCache();
        ResponseHeader responseHeader = header.toResponseHeader();
        int headerSize = responseHeader.size();
        ApiMessage apiMessage = response.data();
        int messageSize = apiMessage.size(cache, apiVersion);
        final ByteBuf buffer = ctx.alloc().buffer(headerSize + messageSize);
        buffer.writerIndex(headerSize + messageSize);
        final ByteBuffer nioBuffer = buffer.nioBuffer();
        final ByteBufferAccessor writable = new ByteBufferAccessor(nioBuffer);
        responseHeader.data().write(writable, cache, apiVersion);
        apiMessage.write(writable, cache, apiVersion);
        return buffer;
    }
}

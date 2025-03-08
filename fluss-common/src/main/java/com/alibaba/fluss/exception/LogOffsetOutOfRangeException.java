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

package com.alibaba.fluss.exception;

import com.alibaba.fluss.annotation.PublicEvolving;

/**
 * No reset policy has been defined, and the offsets for these buckets are either larger or smaller
 * than the range of offsets the server has for the given bucket.
 *
 * @since 0.1
 */
@PublicEvolving
public class LogOffsetOutOfRangeException extends InvalidOffsetException {

    private static final long serialVersionUID = 1L;

    public LogOffsetOutOfRangeException(String message) {
        super(message);
    }

    public LogOffsetOutOfRangeException(String message, Throwable cause) {
        super(message, cause);
    }
}

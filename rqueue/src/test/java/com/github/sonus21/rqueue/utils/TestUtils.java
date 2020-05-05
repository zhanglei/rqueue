/*
 * Copyright 2020 Sonu Kumar
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.sonus21.rqueue.utils;

import com.github.sonus21.rqueue.listener.QueueDetail;
import com.github.sonus21.rqueue.models.db.QueueConfig;

public class TestUtils {
  TestUtils() {}

  public static QueueConfig createQueueConfig(
      String name, int numRetry, boolean delayed, long visibilityTimeout, String dlq) {
    return createQueueDetail(name, numRetry, delayed, visibilityTimeout, dlq).toConfig();
  }

  public static QueueDetail createQueueDetail(
      String name, int numRetry, boolean delayed, long visibilityTimeout, String dlq) {
    return QueueDetail.builder()
        .name(name)
        .queueName("__rq::queue::" + name)
        .processingQueueChannelName("__rq::p-channel::" + name)
        .processingQueueName("__rq::p-queue::" + name)
        .delayedQueueName("__rq::d-queue::" + name)
        .delayedQueueChannelName("__rq::d-channel::" + name)
        .numRetry(numRetry)
        .delayedQueue(delayed)
        .visibilityTimeout(visibilityTimeout)
        .deadLetterQueueName(dlq)
        .build();
  }
}
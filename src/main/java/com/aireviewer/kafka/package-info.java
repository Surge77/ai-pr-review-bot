/**
 * Asynchronous review pipeline backbone: producer, consumer (manual offset
 * commit, retry with backoff), and dead-letter handling for poison messages.
 */
package com.aireviewer.kafka;

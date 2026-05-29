/**
 * Redis-backed review cache: SHA-256 keyed per-file diff lookups that skip
 * unchanged files, with fail-open behavior when Redis is unavailable.
 */
package com.aireviewer.cache;

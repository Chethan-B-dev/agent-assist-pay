local tokens_key = KEYS[1]
local ts_key = KEYS[2]
local capacity = tonumber(ARGV[1])
local refill_rate = tonumber(ARGV[2])
local now = tonumber(ARGV[3])
local requested = tonumber(ARGV[4])
local ttl_seconds = tonumber(ARGV[5])

local last_tokens = tonumber(redis.call('get', tokens_key))
if last_tokens == nil then last_tokens = capacity end

local last_refreshed = tonumber(redis.call('get', ts_key))
if last_refreshed == nil then last_refreshed = now end

local delta = math.max(0, now - last_refreshed)
local refill = delta * refill_rate
local tokens = math.min(capacity, last_tokens + refill)
local allowed = 0

if tokens >= requested then
  allowed = 1
  tokens = tokens - requested
end

redis.call('set', tokens_key, tokens)
redis.call('set', ts_key, now)

if ttl_seconds and ttl_seconds > 0 then
  redis.call('expire', tokens_key, ttl_seconds)
  redis.call('expire', ts_key, ttl_seconds)
end

return {allowed, math.floor(tokens)}
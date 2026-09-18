-- Use a high-cardinality user ID per request. This models independent users and avoids
-- accidentally measuring duplicate-registration rejection instead of slot contention.
math.randomseed(os.time() + math.floor(os.clock() * 1000000))

function request()
  local user_id = math.random(1, 2147483647)
  wrk.method = "POST"
  wrk.path = "/activities/10001/registrations"
  wrk.headers["X-User-Id"] = tostring(user_id)
  return wrk.format(nil, nil, nil, nil)
end

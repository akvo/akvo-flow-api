local cjson= require("cjson")
local openidc = require("resty.openidc")

-- Enable for debugging
-- openidc.set_logging(nil, { DEBUG = ngx.ERR })

function is_empty (s)
   return string.len(string.gsub(s, "%s+", "")) == 0
end

local user_agent = ngx.var.http_user_agent
local accept = ngx.var.http_accept
local authorization = ngx.var.http_authorization

if not user_agent or is_empty(user_agent) then
   ngx.status = 400
   local error = cjson.encode({error="Invalid User-Agent request header"})
   ngx.say(error)
   ngx.exit(ngx.HTTP_BAD_REQUEST)
end

if accept ~= "application/vnd.akvo.flow.v2+json" then
   ngx.status = 400
   local error = cjson.encode({error="Invalid Accept request header"})
   ngx.say(error)
   ngx.exit(ngx.HTTP_BAD_REQUEST)
end

if not authorization or is_empty(authorization) then
   ngx.status = 401
   local error = cjson.encode({error="Authentication required"})
   ngx.say(error)
   ngx.exit(ngx.HTTP_UNAUTHORIZED)
end

local introspection_url = os.getenv("TOKEN_INTROSPECTION_URL")

local secrets_mount_path = os.getenv("SECRETS_MOUNT_PATH")
local file = io.open(secrets_mount_path .. "/keycloak.json", "r")
local json_config = nil
local kc_config = nil

if file then
   json_config = file:read("*a")
   file:close()
end

if json_config then
   kc_config = cjson.decode(json_config)
else
   ngx.status = 500
   ngx.say("Internal server error - Unable to read config")
   ngx.exit(ngx.HTTP_INTERNAL_SERVER_ERROR)
end

local opts = {
   introspection_endpoint = introspection_url,
   introspection_token_param_name = "token",
   client_id = kc_config.resource,
   client_secret = kc_config.credentials.secret,
   ssl_verify = "yes",
   expiry_claim = "exp"
}

local res, err = openidc.introspect(opts)

if err or not res or not res.active then
   ngx.status = 403
   if err then
      local error = cjson.encode({error=err})
      ngx.say(err)
   else
      local error = cjson.encode({error="Invalid access_token"})
      ngx.say(error)
   end
   ngx.exit(ngx.HTTP_FORBIDDEN)
end

ngx.req.set_header("X-Akvo-Email", res.email)

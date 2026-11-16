# Single-node lab agent, isolated high ports. Not -dev: state persists.
server = true
bootstrap_expect = 1
node_name = "soa-lab3"
bind_addr = "127.0.0.1"
advertise_addr = "127.0.0.1"
client_addr = "127.0.0.1"
disable_update_check = true
ui_config { enabled = false }
connect { enabled = false }
ports {
  http = 18500
  https = -1
  dns = -1
  grpc = -1
  grpc_tls = -1
  server = 18300
  serf_lan = 18301
  serf_wan = 18302
}

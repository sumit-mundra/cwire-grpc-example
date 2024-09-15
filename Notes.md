# Notes

By default, grpc uses protocol buffers, as IDL and structure of payload. Other alternatives can be used if needed

## Four types of service method

1. Unary
2. Server Streaming
3. Client Streaming
4. Bidirectional Streaming

Order of message in each stream in #2 to #4 method types are preserved by grpc.

Both async and sync style of RPC invocation is possible.

* Server can reply back with a response message, status, and optional trailing metadata.
* In bidirectional stream, server and client can chat with each other in any order irrespective of which message is delivered first. But, the call is initiated by client first.

Deadlines with each grpc call is possible in following ways depending on language APIs:
* Fixed point in time
* Timeout 

RPCs can be cancelled by either side independently and success criteria are different for both client and server irrespective of each other.

Metadata is info about a rpc call ( key-value pairs ) keys are string. values can be string/binary. key can not start with "grpc-". Binary-valued keys should end in "-bin"

User-defined metadata is not used by gRPC. **Access to metadata is language-dependent**

Channel has two state: connected and idle. gRPC Channel provides a connection to a gRPC server on a host:port.

bitcoin-rpc-client
==================

This is a lightweight java bitcoind JSON-RPC client binding. It does not require any external dependencies.

Maven
=====
The package is published in the wf.bitcoin group and you can add it to you pom.xml adding a section like this:

```
<dependency>
    <groupId>wf.bitcoin</groupId>
    <artifactId>bitcoin-rpc-client</artifactId>
    <version>1.1.0</version>
</dependency>
```


Configuration
=====
In order to know what RPC API to use, the library will look in the bitcoind configuration file (`<user home>/.bitcoin/bitcoin.conf`) and read the relevant configs:
- rpcuser
- rpcpassword
- rpcconnect
- rpcport

Here is a sample bitcoin.conf that will setup bitcoind to run in regtest mode and in a way compatible with this library:

```
# Maintain full transaction index, used in lookups by the getrawtransaction call
txindex=1

# Run bitcoind in regtest mode
regtest=1

# Accept command line and JSON-RPC commands
server=1

# Tells bitcoind that the RPC API settings on the following lines apply to the regtest RPC API
[regtest]

# RPC API settings
rpcuser=myUsername
rpcpassword=myPassword
rpcconnect=localhost
rpcport=9997
```
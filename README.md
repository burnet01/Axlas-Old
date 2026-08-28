# AxlasPermissions

AxlasPermissions is a permissions and player-management library for
[Minestom](https://minestom.net/) servers. It provides rank-based permissions,
player-specific permissions, denied permissions, MongoDB persistence, and
optional Redis caching with an automatic in-memory fallback.

This is an older project, but it should still work fine with the versions and
configuration included here. Please test it against your current Minestom
version before using it in a production server.

## Features

- Rank management with weights, prefixes, colors, and permissions
- Player ranks, extra permissions, and denied permissions
- Exact and wildcard permission checks such as `example.*`
- MongoDB as the persistent source of truth
- Redis caching for shared deployments
- Local in-memory caching when Redis is unavailable
- Built-in Minestom listeners, chat handling, and permission commands

## Requirements

- Java 25
- Maven
- A MongoDB instance
- Redis for shared caching between multiple server instances (optional)
- A Minestom server

## Installation

Build the project with Maven:

```bash
mvn clean package
```

The library artifact is produced in `target/`. To use it from another Maven
project, use the coordinates below after publishing or installing the artifact
in a repository:

```xml
<dependency>
    <groupId>rip.wiped</groupId>
    <artifactId>axlas-permissions</artifactId>
    <version>1.0.0</version>
</dependency>
```

## Configuration

The default configuration is loaded from `src/main/resources/axlas.properties`:

```properties
mongo.uri=mongodb://localhost:27017
mongo.database=axlas_permissions
redis.host=localhost
redis.port=6379
redis.password=
```

Set `mongo.uri` and `mongo.database` for your deployment. Redis is optional:
when it cannot be reached, AxlasPermissions uses a local cache and logs a
warning. A local cache does not provide cache invalidation between multiple
server instances.

When embedding the library, place a configuration file on the classpath and
load it by name:

```java
AxlasPermissions permissions = AxlasPermissions.bootstrap("my-axlas.properties");
```

You can also provide a `java.util.Properties` instance directly:

```java
Properties config = new Properties();
config.setProperty("mongo.uri", "mongodb://localhost:27017");
config.setProperty("mongo.database", "my_server");

AxlasPermissions permissions = AxlasPermissions.from(config);
```

## Using It With Minestom

Initialize Minestom before bootstrapping AxlasPermissions. Bootstrapping wires
the permission listener, chat handler, and built-in commands into the active
Minestom server:

```java
MinecraftServer.init();

AxlasPermissions permissions = AxlasPermissions.bootstrap();
PermissionService service = permissions.getService();

boolean canUseSomething = service.hasPermission(player.getUuid(), "example.use");
```

For a custom Minestom server, keep the returned instance and close it during
shutdown:

```java
permissions.shutdown();
```

The service also exposes rank and player operations, including
`createRank`, `updateRank`, `deleteRank`, `setPlayerRank`,
`addExtraPermission`, `denyPermission`, and `getOrCreatePlayer`.

## Example Server

`rip.wiped.permissions.AxlasServer` is the server's main file and is included
as an example of how to use this library in a Minestom server. It demonstrates
initializing Minestom, bootstrapping AxlasPermissions, registering a basic
instance and spawn handler, starting the server, and shutting the permission
system down cleanly.

It is not required to use `AxlasServer` in your own project. Copy the relevant
integration steps into your existing Minestom server instead. The Maven shade
configuration also uses this class as the executable JAR entry point.

## License

This project is released under the [Unlicense](LICENSE). You may use, modify,
and distribute it without credit or attribution. It is provided as-is, without
warranty, and the authors disclaim liability to the fullest extent permitted by
law.
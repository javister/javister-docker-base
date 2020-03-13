package com.github.javister.docker.testing.base.app;

import io.jooby.Jooby;
import io.jooby.ServerOptions;

public class App extends Jooby {

    App() {
        ServerOptions serverOptions = new ServerOptions();
        serverOptions.setHost("0.0.0.0");
        setServerOptions(serverOptions);
        get("/", ctx -> "Hello, world!");
    }

    public static void main(final String[] args) {
        runApp(args, App::new);
    }
}

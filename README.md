# Coveralls Status Hook

A webhook bridge from Coveralls.io to Github's Status API.

## Prerequisites

You will need [Leiningen][] 2.0.0 or above installed.

[leiningen]: https://github.com/technomancy/leiningen

## Running

To start a web server for the application, run:

    GITHUB_OAUTH_TOKEN=<oauth_token> lein ring server-headless

## License

Copyright © 2015 Michael Smith

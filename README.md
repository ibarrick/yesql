# Yesql - Clojure SQL queries rethought.

Yesql is a Clojure library for /using/ SQL queries.

## Rationale

You're writing Clojure. You need to write some SQL.

I think we're all agreed that this is a problem:

``` clojure
(query "SELECT * FROM users WHERE country_code = ?" "GB")
```

Unless these query strings are short, they quickly get hard to read
and hard to rewrite.

But something like this is not the solution:

``` clojure
(select :*
        (from :users)
        (where (= :country_code "GB")))
```

Clojure is a great language for writing DSLs, but we don't need a new
one. SQL is already a mature DSL.  And s-expressions are great, but
here they're not adding anything. This is parens-for-parens sake.
(Don't agree? Wait until this extra syntax layer breaks down and you
start wrestling with a `(raw-sql)` function.)

So what's the solution? Keep the SQL in SQL. Have one file with your
query:

``` sql
SELECT *
FROM users
WHERE country_code = ?
```

...and then pull it in and use it as a regular Clojure function:

``` clojure
(defquery users-by-country "myproject/db/users-by-country.sql")

(users-by-country db-spec "GB")
```

By keeping the SQL and Clojure separate you get:

- No syntactic surprises. Your database doesn't stick to the SQL
  standard (because none of them do); Yesql doesn't care. You will
  never spend time hunting for "the equivalent sexpr syntax". You will
  never need to fallback to a `(raw-sql "some('funky'::SYNTAX)")` function.
- Better editor support. Your editor probably already has great SQL
  support. By keeping SQL in SQL, you get to use it.
- Team interoperability. Your DBAs can read and write the queries you
  use in your Clojure project.
- Easier performance tuning. Need to `EXPLAIN` that query plan? It's
  much easier when your query is ordinary SQL.
- Query reuse. Drop the same SQL files into other projects, because
  they're just plain ol' SQL. Share them as a submodule.

## Installation

[Leiningen](https://github.com/technomancy/leiningen) dependency information:

``` clojure
[yesql "0.1.0-SNAPSHOT"]
```

## Example usage

Create an SQL query:

```sql
-- Counts the users in a given country.
SELECT count(*) AS count
FROM user
WHERE country_code = :country_code
```

```clojure
; Define a database connection spec. (This is standard clojure.java.jdbc.)
(def db-spec {:classname "org.postgresql.Driver"
              :subprotocol "postgresql"
              :subname "//localhost:5432/demo"
              :user "me"})

; Import the SQL query as a function.
(defquery users-by-country "some/where/users_by_country.sql")

; Lo! It has automatic docstrings in the REPL:
(clojure.repl/find-doc "users-by-country")

; user> -------------------------
; user> user/users-by-country
; user> ([db country_code])
; user>   Counts the users in a given country.

; Use it.
(users-by-country db-spec "GB")

; user> ({:count 58})

; Use it in a transaction.
(sql/db-transaction [connection db-spec]
                    {:limeys (users-by-country connection "GB") 
                     :yanks  (users-by-country connection "US")})
```

## Status

In development. I welcome your feedback.

## License

Copyright © 2013 Kris Jenkins

Distributed under the Eclipse Public License, the same as Clojure.
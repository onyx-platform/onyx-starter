# onyx-starter

A starter project to get your feet wet with Onyx `0.5.2`. Uses the core.async plugin for both input and output. Requires no external dependencies.

## Walk Through

[WALKTHROUGH.md](WALKTHROUGH.md) walks through the repository in detail.

## Usage

##### Clone the repo

`git clone git@github.com:MichaelDrogalis/onyx-starter.git`

##### Run the sample

Evaluate it bit-by-bit in the repl. Tail `onyx.log` for Onyx output.

##### Expected output

```clojure
[{:word "HeY!"}
 {:word "ThErE!"}
 {:word "UsEr!"}
 {:word "It's!"}
 {:word "ReAlLy!"}
 {:word "NiCe!"}
 {:word "OuTsIdE!"}
 {:word "I!"}
 {:word "LiVe!"}
 {:word "In!"}
 {:word "ReDmOnD!"}
 :done]

[{:word "HeY?"}
 {:word "ThErE?"}
 {:word "UsEr?"}
 {:word "It's?"}
 {:word "ReAlLy?"}
 {:word "NiCe?"}
 {:word "OuTsIdE?"}
 {:word "I?"}
 {:word "LiVe?"}
 {:word "In?"}
 {:word "ReDmOnD?"}
 :done]
```

##### Dashboard

If you would like to use the [Onyx
Dashboard](https://github.com/lbradstreet/onyx-dashboard/) to inspect the
cluster, download the
[uberjar](https://github.com/lbradstreet/onyx-dashboard/#deployment).

As the dashboard is unable to use VM mode HornetQ, you will have to run a standalone HornetQ instance. You can do so by:

1. cd hornetq
2. ./setup-hq.sh
3. ./run-hq.sh

In core.clj, alter the line:
`(def vm-hornetq? true)` 
to 
`(def vm-hornetq?  false)`.  
Then evaluate the sample file bit by bit, as above.
After the line: 
`(def env (onyx.api/start-env env-config))`
start the dashboard by:

INSERT JAR RUNNING HERE.

## License

Copyright © 2014 Michael Drogalis

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.

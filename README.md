## What is ScaMATA ?

ScaMATA is a library of algorithms which aim at allocating some tasks to some agents.

We have implemented our prototype with the
[Scala](https://www.scala-lang.org/) programming language and the
[Akka](http://akka.io/) toolkit. The latter, which is based on the
actor model, allows us to fill the gap between the specification and
its implementation.

## Requirements

In order to run the demonstration you need: 

- the Java virtual machine [JVM 1.8.0_60](http://www.oracle.com/technetwork/java/javase/downloads/index.html).

- [IBM ILOG CPLEX Optimization Studio 12.8](https://www.ibm.com/analytics/data-science/prescriptive-analytics/cplex-optimizer) 

In order to compile the code you need:

- the programming language [Scala 2.11.8](http://www.scala-lang.org/download/);

- the interactive build tool [SBT 0.13](http://www.scala-sbt.org/download.html).

## Test

    java -jar ScaMATA-assembly-X.Y.jar org.scamata.util.MWTAPSolver -v -d examples/toy4x4.txt  examples/toy4x4Cmax.txt

Usage: 

    Usage: java -jar ScaMWTA-assembly-X.Y.jar [-v] inputFilename outputFilename
    The following options are available:
    -v: verbose
    -d: distributed (false by default)
    -f: LC (LCmax by default)

## Installation

Add to ~/.sbt/0.13/global.sbt

    resolvers += "Artima Maven Repository" at "http://repo.artima.com/releases"

Compile

    sbt compile

then

    sbt "run org.scamata.util.MATASolver -v -d examples/toy4x4.txt  examples/toy4x4Cmax.txt"
 
and eventually

    sbt assembly


## Contributors

Copyright (C) Maxime MORGE 2018

## License

This program is free software: you can redistribute it and/or modify it under the terms of the 
GNU General Public License as published by the Free Software Foundation, either version 3 of the License, 
or (at your option) any later version.

This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; 
without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  
See the GNU General Public License for more details.

You should have received a copy of the GNU General Public License along with this program.  
If not, see <http://www.gnu.org/licenses/>.

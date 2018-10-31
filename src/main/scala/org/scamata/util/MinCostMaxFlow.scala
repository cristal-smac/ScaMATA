  // Copyright (C) Maxime MORGE 2018
  package org.scamata.util

  /**
    * Ford-Fulkerson algorithm to find a minimal-cost maximal flow
    * Running time: O(min(|V|^2, |V|^3)
    *
    * @param capacity a matrix such that cap[i][j] is the capacity of a directed edge from node i to node j
    * @param cost     a matrix such that cost[i][j] is the (positive) cost of sending one unit of flow along a directed edge from node i to node j
    * @param source   the index of the source vertex
    * @param sink   the index of the sink vertex
    */
  class MinCostMaxFlow(capacity : Array[Array[Int]], cost: Array[Array[Double]], source: Int, sink: Int) {

    val N = capacity.length //The implicit number of vertex
    var found: Array[Boolean] = new Array[Boolean](N)
    var flow = Array.ofDim[Int](N, N)

    var dist = new Array[Double](N + 1)
    var dad = new Array[Int](N)
    var pi = new Array[Double](N)
    var totalFlow = 0
    var totalCost = 0.0

    /**
      * Execute the minimal-cost maximal flow algorithm and returns
      * @return totalCost, totalFlow
      */
    def getMaxFlow() : (Int, Double) = {
      while (search(source,sink)){
        var residualCapacity =Integer.MAX_VALUE
        var x = sink
        while(x != source){
          residualCapacity = Math.min(residualCapacity, if (flow(x)(dad(x)) != 0) flow(x)(dad(x))
          else capacity(dad(x))(x) - flow(dad(x))(x))
          x = dad(x)
        }
        x = sink
        while (x != source) {
          if (flow(x)(dad(x)) != 0) {
            flow(x)(dad(x)) -= residualCapacity
            totalCost -= residualCapacity * cost(x)(dad(x))
          } else {
            flow(dad(x))(x) += residualCapacity
            totalCost += residualCapacity * cost(dad(x))(x)
          }
          x = dad(x)
        }
        totalFlow += residualCapacity
      }
      (totalFlow, totalCost)
    }


    /**
      * Returns true if there is a path from source 's' to sink 't' in
      * residual graph. Also fills ?[] to store the path
       */
    def search(source: Int, sink: Int): Boolean = {
      var src = source
      found = Array.fill(N)(false)
      dist = Array.fill(N+1)(Integer.MAX_VALUE)
      dist(src) = 0
      while (src != N) {
        var best = N
        found(src) = true
        for (k <- 0 until N) {
          //if (found[k]) continue
          if (!found(k)) {
          if (flow(k)(src) != 0) {
            val currentValue = dist(src) + pi(src) - pi(k) - cost(k)(src)
            if (dist(k) > currentValue){
              dist(k) = currentValue
              dad(k) = src
            }
          }
          if (flow(src)(k) < capacity(src)(k)){
            val currentValue = dist(src) + pi(src) - pi(k) + cost(src)(k)
            if (dist(k) > currentValue){
              dist(k) = currentValue
              dad(k) = src
            }
          }
          if (dist(k) < dist(best)) best = k
        }
      }
        src = best
      }
      for (k <- 0 until N) {
        pi(k) = Math.min(pi(k) + dist(k), Double.MaxValue)
      }
        found(sink)
      }
    }



  /**
    * Companion object to test it
    */
  object MinCostMaxFlow {
      def main(args: Array[String]): Unit = {
        val capacity = Array(
          Array(0, 3, 4, 5, 0),
          Array(0, 0, 2, 0, 0),
          Array(0, 0, 0, 4, 1),
          Array(0, 0, 0, 0, 10),
          Array(0, 0, 0, 0, 0))
        val cost1 = Array(
          Array(0.0, 1.0, 0.0, 0.0, 0.0),
          Array(0.0, 0.0, 0.0, 0.0, 0.0),
          Array(0.0, 0.0, 0.0, 0.0, 0.0),
          Array(0.0, 0.0, 0.0, 0.0, 0.0),
          Array(0.0, 0.0, 0.0, 0.0, 0.0))
        val cost2 = Array(
          Array(0.0, 0.0, 1.0, 0.0, 0.0),
          Array(0.0, 0.0, 0.0, 0.0, 0.0),
          Array(0.0, 0.0, 0.0, 0.0, 0.0),
          Array(0.0, 0.0, 0.0, 0.0, 0.0),
          Array(0.0, 0.0, 0.0, 0.0, 0.0))
        var flow = new MinCostMaxFlow(capacity, cost1, 0, 4)
        System.out.println(flow.getMaxFlow()) // 10 1
        flow = new MinCostMaxFlow(capacity, cost2, 0, 4)
        println(flow.getMaxFlow()) // 10 3
      }
    }

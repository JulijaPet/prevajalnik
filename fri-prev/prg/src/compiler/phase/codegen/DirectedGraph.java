package compiler.phase.codegen;

import java.util.*;

public class DirectedGraph<V> {

    public static class Edge<V>{
        public V vertex;

        public Edge(V v){
            vertex = v;
        }

        public V getVertex() {
            return vertex;
        }

        @Override
        public String toString() {
            return "[ " + vertex + " ]";
        }

    }

    public Map<V, List<Edge<V>>> neighbors = new HashMap<V, List<Edge<V>>>();

    public String toString() {
        StringBuffer s = new StringBuffer();
        for (V v : neighbors.keySet())
            s.append("\n    " + v + " -> " + neighbors.get(v));
        return s.toString();
    }


    public void add(V vertex) {
        if (neighbors.containsKey(vertex))
            return;
        neighbors.put(vertex, new ArrayList<Edge<V>>());
    }

    public int getNumberOfEdges(){
        int sum = 0;
        for(List<Edge<V>> outBounds : neighbors.values()){
            sum += outBounds.size();
        }
        return sum;
    }

    public boolean contains(V vertex) {
        return neighbors.containsKey(vertex);
    }

    public void add(V from, V to) {
        this.add(from);
        this.add(to);
        neighbors.get(from).add(new Edge<V>(to));
    }
    
    public void remove (V from, V to) {
        if (!(this.contains(from) && this.contains(to)))
            throw new IllegalArgumentException("Nonexistent vertex");
        neighbors.get(from).remove(to);
    }

    public void remove(V vertex) {
    	if (!(this.contains(vertex)))
            throw new IllegalArgumentException("Nonexistent vertex");
        neighbors.remove(vertex);
    }
    
    public int outDegree(String vertex) {
        return neighbors.get(vertex).size();
    }

    public int inDegree(V vertex) {
       return inboundNeighbors(vertex).size();
    }

    public List<V> outboundNeighbors(V vertex) {
        List<V> list = new ArrayList<V>();
        for(Edge<V> e: neighbors.get(vertex))
            list.add(e.vertex);
        return list;
    }

    public List<V> inboundNeighbors(V inboundVertex) {
        List<V> inList = new ArrayList<V>();
        for (V to : neighbors.keySet()) {
            for (Edge e : neighbors.get(to))
                if (e.vertex.equals(inboundVertex))
                    inList.add(to);
        }
        return inList;
    }

    public boolean isEdge(V from, V to) {
      for(Edge<V> e :  neighbors.get(from)){
          if(e.vertex.equals(to))
              return true;
      }
      return false;
    }




}
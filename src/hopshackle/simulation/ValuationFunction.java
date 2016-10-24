package hopshackle.simulation;

public interface ValuationFunction<T> {

	double getValue(T item);
	
	String toString(T stuff);
}


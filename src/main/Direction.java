package main;

public enum Direction {
	OUT(0), IN(1);
	
	public int _val;
	
	private Direction(final Integer val) {
	    this._val = val;
	}
}

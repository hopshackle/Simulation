package hopshackle.simulation;

public class InvalidStateTransition extends RuntimeException {
	private static final long serialVersionUID = -8459076735758399835L;
	public InvalidStateTransition(String message) {
		super(message);
	}
}

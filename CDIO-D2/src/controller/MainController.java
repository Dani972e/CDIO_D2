package controller;

import socket.ISocketController;
import socket.ISocketObserver;
import socket.SocketInMessage;
import socket.SocketOutMessage;
import weight.IWeightInterfaceController;
import weight.IWeightInterfaceObserver;
import weight.KeyPress;

/**
 * MainController - integrating input from socket and ui. Implements
 * ISocketObserver and IUIObserver to handle this.
 * 
 * @author Christian Budtz
 * @version 0.1 2017-01-24
 *
 */
public class MainController implements IMainController, ISocketObserver, IWeightInterfaceObserver {

	private ISocketController socketHandler;
	private IWeightInterfaceController weightController;
	private KeyState keyState = KeyState.K1;
	private double brutto, tara;

	public MainController(ISocketController socketHandler, IWeightInterfaceController weightInterfaceController) {
		this.init(socketHandler, weightInterfaceController);
	}

	@Override
	public void init(ISocketController socketHandler, IWeightInterfaceController weightInterfaceController) {
		this.socketHandler = socketHandler;
		this.weightController = weightInterfaceController;
	}

	@Override
	public void start() {
		if (socketHandler != null && weightController != null) {
			// Makes this controller interested in messages from the socket
			socketHandler.registerObserver(this);
			// Starts socketHandler in own thread
			new Thread(socketHandler).start();
			// Makes this controller interested in messages from the socket
			weightController.registerObserver(this);
			// Starts weightController in own thread
			new Thread(weightController).start();
		} else {
			System.err.println("No controllers injected!");
		}
	}

	// Listening for socket input
	@Override
	public void notify(SocketInMessage message) {
		switch (message.getType()) {
		case B:
			// Sætter brutto på vægt simulator til det givne antal kg.
			// eks. B 1.234 crlf
			weightController.showMessagePrimaryDisplay(message.getMessage());
			break;
		case D:
			// Max 7 characters is showed onto the display. Virker. (håber vi) Skriver vægt i display.
			weightController.showMessagePrimaryDisplay(message.getMessage());
			break;
		case Q:
			// Programmet skal afsluttes.
			System.exit(0);
			break;
		case RM204:
			weightController.showMessageSecondaryDisplay(message.getMessage());
			break;
		case RM208:
			// Skriv i displayet og afvent indtastning.
			// Vægten viser en besked til brugeren og afventer brugerens input.
			// Der sendes en bekræftelse. Når brugerens input er afsluttet sendes dette. 
			// NB. Der er 2 svar fra vægten.
			weightController.showMessagePrimaryDisplay("Afventer indtastning..");
			
			
			break;
		case S:
			// Send stabil afvejning. 
			socketHandler.sendMessage(new SocketOutMessage("S S      " + (brutto - tara))); // netto
			break;
		case T:
			// Vægt tarares.
            tara = brutto;
            weightController.showMessagePrimaryDisplay("0.000kg");
            socketHandler.sendMessage(new SocketOutMessage("T S      " + tara + " kg" ));
			break;
		case DW:
			// Vægtens display ryddes og vægten svarer med en bekræftelse.
			weightController.showMessagePrimaryDisplay("");
			break;
		case K:
			// K skifter vægtens knap tilstand. Når der trykkes på funktionstaster (tara, zero, [->, send)
			// afhænger resultatet af vægtens tilstand.
			// 
			handleKMessage(message);
			break;
		case P111:
			// Skriver max 30 tegn i sekundært display.
			weightController.showMessageSecondaryDisplay(message.getMessage());
			break;
		}

	}

	private void handleKMessage(SocketInMessage message) {
		switch (message.getMessage()) {
		case "1":
			this.keyState = KeyState.K1;
			break;
		case "2":
			this.keyState = KeyState.K2;
			break;
		case "3":
			this.keyState = KeyState.K3;
			break;
		case "4":
			this.keyState = KeyState.K4;
			break;
		default:
			socketHandler.sendMessage(new SocketOutMessage("ES"));
			break;
		}
	}

	// Listening for UI input
	@Override
	public void notifyKeyPress(KeyPress keyPress) {
		// TODO implement logic for handling input from ui
		System.out.println("Tastet: " + keyPress.getCharacter());
		switch (keyPress.getType()) {
		case SOFTBUTTON:
			break;
		case TARA:
			break;
		case TEXT:
			break;
		case ZERO:
			break;
		case C:
			break;
		case EXIT:
			System.exit(0);
			break;
		case SEND:
			if (keyState.equals(KeyState.K4) || keyState.equals(KeyState.K3)) {
				socketHandler.sendMessage(new SocketOutMessage("K A 3"));
			}
			break;
		}

	}

	@Override
	public void notifyWeightChange(double newWeight) {
		// HOLD STYR PÅ BRUTTO BELASTNING
		weightController.showMessagePrimaryDisplay("" + newWeight);
		brutto = newWeight;
	}

}
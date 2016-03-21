//--------------------------------------------------// 
//SYSC 3303 Iteration 4 							//
//TFTP Server Program: InvalidRequestException.java	//
//													//
//Author: Jonathan Chan								//
//Student Number: 100936881							//
//													//
//Carleton University								//
//Department of Systems and Computer Engineering	//
//SYSC 3303 RealTime								//
//Concurrent Systems Winter 2016					//
//--------------------------------------------------//

public class InvalidRequestException extends Exception {

	private static final long serialVersionUID = 1L;

	public InvalidRequestException(String msg){
		super(msg);
	}
}

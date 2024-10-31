package kaphein.jpa.mql;

public class MqlException extends RuntimeException
{
  private static final long serialVersionUID = 4042049080250773779L;

  MqlException(String message)
  {
    super(message);
  }
}

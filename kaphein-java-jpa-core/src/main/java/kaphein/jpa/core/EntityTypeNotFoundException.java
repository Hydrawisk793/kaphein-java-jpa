package kaphein.jpa.core;

public class EntityTypeNotFoundException extends RuntimeException
{
  /**
   * @hidden
   */
  EntityTypeNotFoundException(Throwable cause)
  {
    super(cause);
  }

  EntityTypeNotFoundException(
    String message,
    Throwable cause
  )
  {
    super(message, cause);
  }

  private static final long serialVersionUID = 1991784323654479304L;
}

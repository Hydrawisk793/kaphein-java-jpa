package kaphein.jpa.core;

public class ArgumentNullException extends RuntimeException
{
  /**
   * @hidden
   */
  public ArgumentNullException(String argumentName)
  {
    this.argumentName = argumentName;
  }

  public String getArgumentName()
  {
    return argumentName;
  }

  @Override
  public String getMessage()
  {
    String message = "An argument is null.";

    if(StringUtils.isNotBlank(argumentName))
    {
      message = String.format("Argument '%s' is null.", argumentName);
    }

    return message;
  }

  private static final long serialVersionUID = -8194207814818365641L;

  private final String argumentName;
}

package kaphein.jpa.core;

public class ArgumentBlankException extends RuntimeException
{
  /**
   * @hidden
   */
  public ArgumentBlankException(String argumentName)
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
    String message = "An argument is blank.";

    if(StringUtils.isNotBlank(argumentName))
    {
      message = String.format("Argument '%s' is blank.", argumentName);
    }

    return message;
  }

  private static final long serialVersionUID = -8194207814818365641L;

  private final String argumentName;
}

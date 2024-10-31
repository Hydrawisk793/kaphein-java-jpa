package kaphein.jpa.core;

/**
 * @hidden
 */
public final class AssertArg
{
  public static <T> T isNotNull(T arg, String argName)
  {
    if(null == arg)
    {
      throw new ArgumentNullException(argName);
    }

    return arg;
  }

  public static String isNotBlank(String arg, String argName)
  {
    if(StringUtils.isBlank(arg))
    {
      throw new ArgumentBlankException(argName);
    }

    return arg;
  }

  private AssertArg()
  {
    // Does nothing.
  }
}

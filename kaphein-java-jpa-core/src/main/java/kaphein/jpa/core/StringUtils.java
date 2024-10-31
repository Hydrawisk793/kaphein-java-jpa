package kaphein.jpa.core;

/**
 * @hidden
 */
public final class StringUtils
{
  public static boolean isBlank(CharSequence s)
  {
    boolean result = null == s;
    if(!result)
    {
      final int l = s.length();
      result = l < 1;
      if(!result)
      {
        result = true;
        for(int i = 0; result && i < l; ++i)
        {
          result = Character.isWhitespace(s.charAt(i));
        }
      }
    }

    return result;
  }

  public static boolean isNotBlank(CharSequence s)
  {
    return !isBlank(s);
  }

  private StringUtils()
  {
    // Does nothing.
  }
}

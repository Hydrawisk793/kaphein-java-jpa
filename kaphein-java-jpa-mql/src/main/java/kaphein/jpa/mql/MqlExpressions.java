package kaphein.jpa.mql;

import java.util.LinkedHashMap;
import java.util.Map;

public final class MqlExpressions
{
  public static Map<String, Object> of(Object... params)
  {
    final var expr = new LinkedHashMap<String, Object>();

    if(0 != (params.length & 0x01))
    {
      throw new IllegalArgumentException("The number of parameters must be even");
    }

    for(var i = 0; i < params.length; i += 2)
    {
      final var key = params[i];
      final var value = params[i + 1];

      if(!(key instanceof String))
      {
        throw new IllegalArgumentException("Key parameters must be strings.");
      }

      expr.put((String)key, value);
    }

    return expr;
  }
}

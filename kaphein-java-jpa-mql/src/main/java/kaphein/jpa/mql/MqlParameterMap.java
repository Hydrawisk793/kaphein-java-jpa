package kaphein.jpa.mql;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

class MqlParameterMap implements Map<String, Object>
{
  public MqlParameterMap()
  {
    paramMap = new LinkedHashMap<>();
    paramNameFormat = "__param%d";
    paramNameSeq = 1;
  }

  @Override
  public int size()
  {
    return paramMap.size();
  }

  @Override
  public boolean isEmpty()
  {
    return paramMap.isEmpty();
  }

  @Override
  public boolean containsKey(Object key)
  {
    return paramMap.containsKey(key);
  }

  @Override
  public boolean containsValue(Object value)
  {
    return paramMap.containsValue(value);
  }

  @Override
  public Object get(Object key)
  {
    return paramMap.get(key);
  }

  /**
   * NOT SUPPORTED. Use {@link issueParameterName} method instead.
   */
  @Override
  public Object put(String key, Object value)
  {
    throw new UnsupportedOperationException();
  }

  /**
   * NOT SUPPORTED. Use {@link issueParameterName} method instead.
   */
  @Override
  public void putAll(Map<? extends String, ? extends Object> m)
  {
    throw new UnsupportedOperationException();
  }

  public String issueParameterName(Object parameterValue)
  {
    final var paramName = String.format(paramNameFormat, paramNameSeq++);
    paramMap.put(paramName, parameterValue);

    return paramName;
  }

  @Override
  public Object remove(Object key)
  {
    return paramMap.remove(key);
  }

  @Override
  public void clear()
  {
    paramMap.clear();
  }

  @Override
  public Set<String> keySet()
  {
    return paramMap.keySet();
  }

  @Override
  public Collection<Object> values()
  {
    return paramMap.values();
  }

  @Override
  public Set<Entry<String, Object>> entrySet()
  {
    return paramMap.entrySet();
  }

  private final Map<String, Object> paramMap;

  private final String paramNameFormat;

  private int paramNameSeq;
}

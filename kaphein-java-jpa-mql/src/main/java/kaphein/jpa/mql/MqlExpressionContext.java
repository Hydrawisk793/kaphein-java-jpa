package kaphein.jpa.mql;

import java.util.List;
import java.util.Map;

class MqlExpressionContext
{
  public static enum Kind
  {
    CONDITION_TERMS,

    CLAUSE,

    VALUE_OPERATOR;
  }

  MqlExpressionContext(
    Kind kind,
    MqlExpressionContext parent,
    MqlAstNode currentNode,
    String label,
    Object value
  )
  {
    this.kind = kind;
    this.parent = parent;
    this.currentNode = currentNode;
    this.label = label;
    this.value = value;
  }

  public Kind getKind()
  {
    return kind;
  }

  public MqlExpressionContext getParent()
  {
    return parent;
  }

  public MqlAstNode getCurrentNode()
  {
    return currentNode;
  }

  public String getLabel()
  {
    return label;
  }

  public Object getValue()
  {
    return value;
  }

  @SuppressWarnings("unchecked")
  public Map<String, Object> getExpression()
  {
    final var value = getValue();

    if(!(value instanceof Map))
    {
      throw new RuntimeException();
    }

    return (Map<String, Object>)value;
  }

  @SuppressWarnings("unchecked")
  public List<Map<String, Object>> getExpressions()
  {
    final var value = getValue();

    if(!(value instanceof List))
    {
      throw new RuntimeException();
    }

    return (List<Map<String, Object>>)value;
  }

  private final Kind kind;

  private final MqlExpressionContext parent;

  private final String label;

  private final MqlAstNode currentNode;

  private final Object value;
}

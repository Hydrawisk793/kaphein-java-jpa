package kaphein.jpa.mql;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import kaphein.jpa.mql.MqlAstNode.Kind;

class MqlAstNodeRegistry
{
  MqlAstNodeRegistry()
  {
    nodeMap = new HashMap<>();
  }

  public MqlAstNode create(
    Kind kind,
    String label
  )
  {
    final var id = UUID.randomUUID().toString();
    final var node = new MqlAstNode(
      this,
      id,
      kind,
      label);

    nodeMap.put(id, node);

    return node;
  }

  public MqlAstNode create(
    Kind kind,
    String label,
    Object value
  )
  {
    final var id = UUID.randomUUID().toString();
    final var node = new MqlAstNode(
      this,
      id,
      kind,
      label,
      value);

    nodeMap.put(id, node);

    return node;
  }

  public MqlAstNode get(String id)
  {
    return nodeMap.get(id);
  }

  private final Map<String, MqlAstNode> nodeMap;
}

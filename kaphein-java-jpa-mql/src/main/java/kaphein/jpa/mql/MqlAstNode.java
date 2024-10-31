package kaphein.jpa.mql;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;

import jakarta.persistence.metamodel.EntityType;

class MqlAstNode implements Iterable<MqlAstNode>
{
  public static enum Kind
  {
    ROOT,

    CLAUSE,

    TERM,

    VALUE_OPERATOR,

    ATTRIBUTE_PATH,

    LITERAL;
  }

  MqlAstNode(
    MqlAstNodeRegistry nodeRegistry,
    String id,
    Kind kind,
    String label
  )
  {
    this(
      nodeRegistry,
      id,
      null,
      null,
      kind,
      label,
      null);
  }

  MqlAstNode(
    MqlAstNodeRegistry nodeRegistry,
    String id,
    Kind kind,
    String label,
    Object value
  )
  {
    this(
      nodeRegistry,
      id,
      null,
      null,
      kind,
      label,
      value);
  }

  private MqlAstNode(
    MqlAstNodeRegistry nodeRegistry,
    String id,
    MqlAstNode parent,
    List<MqlAstNode> children,
    Kind kind,
    String label,
    Object value
  )
  {
    this.nodeRegistry = nodeRegistry;
    this.id = id;
    this.parent = parent;
    this.children = new ArrayList<>();
    Optional
      .ofNullable(children)
      .ifPresent(v -> v.forEach(this::addChild));
    this.kind = kind;
    this.label = label;
    this.value = value;

    entityTypeByAliasMap = null;
    alias = null;
    jpqlExpression = null;
  }

  public MqlAstNodeRegistry getNodeRegistry()
  {
    return nodeRegistry;
  }

  public String getId()
  {
    return id;
  }

  public MqlAstNode getParent()
  {
    return parent;
  }

  public boolean isLeaf()
  {
    return children.isEmpty();
  }

  public int getChildCount()
  {
    return children.size();
  }

  @Override
  public Iterator<MqlAstNode> iterator()
  {
    return postOrderIterator();
  }

  public Iterator<MqlAstNode> preOrderIterator()
  {
    return new PreOrderIterator(this);
  }

  public Iterator<MqlAstNode> postOrderIterator()
  {
    return new PostOrderIterator(this);
  }

  public MqlAstNode getChildNodeAt(int index)
  {
    return children.get(index);
  }

  public Optional<MqlAstNode> getFirstChild()
  {
    return children.stream().findFirst();
  }

  public Optional<MqlAstNode> getLastChild()
  {
    final var lastChildIndex = children.size() - 1;

    return Optional.ofNullable((lastChildIndex >= 0
      ? children.get(lastChildIndex)
      : null));
  }

  public int findChildIndexOf(MqlAstNode child)
  {
    return children.indexOf(child);
  }

  public boolean hasChild(MqlAstNode child)
  {
    return findChildIndexOf(child) >= 0;
  }

  void addChild(MqlAstNode c)
  {
    if(null == c)
    {
      throw new IllegalArgumentException();
    }

    if(!hasChild(c))
    {
      children.add(c);
    }

    if(!equals(c.getParent()))
    {
      c.setParent(this);
    }
  }

  void removeChild(MqlAstNode c)
  {
    if(null == c)
    {
      throw new IllegalArgumentException();
    }

    if(hasChild(c))
    {
      children.remove(c);
    }

    if(equals(c.getParent()))
    {
      c.setParent(null);
    }
  }

  public Kind getKind()
  {
    return kind;
  }

  void setKind(Kind kind)
  {
    this.kind = Objects.requireNonNull(kind);
  }

  public String getLabel()
  {
    return label;
  }

  void setLabel(String label)
  {
    this.label = label;
  }

  public Object getValue()
  {
    return value;
  }

  void setValue(Object value)
  {
    this.value = value;
  }

  Map<String, EntityType<?>> getEntityTypeByAliasMap()
  {
    return entityTypeByAliasMap;
  }

  void setEntityTypeByAliasMap(Map<String, EntityType<?>> entityTypeByAliasMap)
  {
    this.entityTypeByAliasMap = entityTypeByAliasMap;
  }

  String getAlias()
  {
    return alias;
  }

  void setAlias(String alias)
  {
    this.alias = alias;
  }

  String getJpqlExpression()
  {
    return jpqlExpression;
  }

  void setJpqlExpression(String jpqlExpression)
  {
    this.jpqlExpression = jpqlExpression;
  }

  private static class PreOrderIterator implements Iterator<MqlAstNode>
  {
    private PreOrderIterator(MqlAstNode rootNode)
    {
      nodeStack = new LinkedList<>();
      nodeStack.push(rootNode);
    }

    @Override
    public boolean hasNext()
    {
      return !nodeStack.isEmpty();
    }

    @Override
    public MqlAstNode next()
    {
      if(!hasNext())
      {
        throw new NoSuchElementException();
      }

      MqlAstNode result = null;

      while(null == result && !nodeStack.isEmpty())
      {
        final var node = nodeStack.pop();

        result = node;

        if(!node.isLeaf())
        {
          final var n = node.getChildCount();
          for(int j = n, i = 0; i < n; ++i)
          {
            --j;
            nodeStack.push(node.getChildNodeAt(j));
          }
        }
      }

      return result;
    }

    private final LinkedList<MqlAstNode> nodeStack;
  }

  private static class PostOrderIterator implements Iterator<MqlAstNode>
  {
    private PostOrderIterator(MqlAstNode rootNode)
    {
      nodeStack = new LinkedList<>();
      nodeStack.push(rootNode);
      lastVisitedNode = null;
    }

    @Override
    public boolean hasNext()
    {
      return !nodeStack.isEmpty();
    }

    @Override
    public MqlAstNode next()
    {
      if(!hasNext())
      {
        throw new NoSuchElementException();
      }

      MqlAstNode result = null;

      while(null == result && !nodeStack.isEmpty())
      {
        final var current = nodeStack.peek();
        if(!current.isLeaf() && current.getLastChild().orElse(null) != lastVisitedNode)
        {
          for(int s = current.getChildCount(), i = s; i > 0;)
          {
            --i;
            final var childNode = current.getChildNodeAt(i);
            nodeStack.push(childNode);
          }
        }
        else
        {
          result = current;

          lastVisitedNode = current;
          nodeStack.pop();
        }
      }

      return result;
    }

    private final LinkedList<MqlAstNode> nodeStack;

    private MqlAstNode lastVisitedNode;
  }

  private void setParent(MqlAstNode parent)
  {
    final var oldParent = this.parent;

    this.parent = parent;

    if(null != oldParent && oldParent != parent && oldParent.hasChild(this))
    {
      oldParent.removeChild(this);
    }

    if(null != parent && !parent.hasChild(this))
    {
      parent.addChild(this);
    }
  }

  private final MqlAstNodeRegistry nodeRegistry;

  private final String id;

  private MqlAstNode parent;

  private final List<MqlAstNode> children;

  private Kind kind;

  private String label;

  private Object value;

  private Map<String, EntityType<?>> entityTypeByAliasMap;

  private String alias;

  private String jpqlExpression;
}

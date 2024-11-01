package kaphein.jpa.mql;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.EntityType;
import kaphein.jpa.core.JpaEntityAttributePath;
import kaphein.jpa.core.MetamodelUtils;

class MqlSubQueryContext
{
  MqlSubQueryContext(
    MqlQueryBuilderContext queryBuilderContext,
    MqlAstNode subQueryRootNode
  )
  {
    this.queryBuilderContext = queryBuilderContext;
    parent = null;
    children = new ArrayList<>();

    this.subQueryRootNode = subQueryRootNode;
    entityTypeByAliasMap = new HashMap<>();

    final var metamodel = queryBuilderContext.getMetamodel();
    final var entityTypeByEntityName = queryBuilderContext.getEntityTypeByEntityNameMap();

    final var entityNameNode = subQueryRootNode.getChildNodeAt(0);
    if(!"entityName".equals(entityNameNode.getLabel()))
    {
      throw new MqlSyntaxException("entityName must be specified");
    }
    final var entityName = (String)entityNameNode.getValue();
    final var entityType = (entityTypeByEntityName.containsKey(entityName)
      ? entityTypeByEntityName.get(entityName)
      : MetamodelUtils.findEntityTypeByEntityNameOrThrow(metamodel, entityName));
    this.entityNameNode = entityNameNode;
    this.entityType = entityType;

    final var aliasNode = subQueryRootNode.getChildNodeAt(1);
    if(!"alias".equals(aliasNode.getLabel()))
    {
      throw new MqlSyntaxException("alias must be specified");
    }
    final var alias = (String)aliasNode.getValue();
    this.aliasNode = aliasNode;
    this.alias = alias;

    final var queryNode = subQueryRootNode.getChildNodeAt(2);
    if(!"jpqlWhere".equals(queryNode.getLabel()))
    {
      throw new MqlSyntaxException("query must be specified");
    }
    if(1 != queryNode.getChildCount())
    {
      throw new MqlSyntaxException("Invalid query syntax");
    }
    this.queryNode = queryNode;

    putEntityTypeByAlias(alias, entityType);

    entityTypeByEntityName.putIfAbsent(entityName, entityType);
  }

  public MqlQueryBuilderContext getQueryBuilderContext()
  {
    return queryBuilderContext;
  }

  public MqlSubQueryContext getParent()
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

  public Optional<MqlSubQueryContext> getFirstChild()
  {
    return children.stream().findFirst();
  }

  public Optional<MqlSubQueryContext> getLastChild()
  {
    final var lastChildIndex = children.size() - 1;

    return Optional.ofNullable((lastChildIndex >= 0
      ? children.get(lastChildIndex)
      : null));
  }

  public int findChildIndexOf(MqlSubQueryContext child)
  {
    return children.indexOf(child);
  }

  public boolean hasChild(MqlSubQueryContext child)
  {
    return findChildIndexOf(child) >= 0;
  }

  void addChild(MqlSubQueryContext c)
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

  void removeChild(MqlSubQueryContext c)
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

  private void setParent(MqlSubQueryContext parent)
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

  public MqlAstNode getSubQueryRootNode()
  {
    return subQueryRootNode;
  }

  MqlAstNode getEntityNameNode()
  {
    return entityNameNode;
  }

  public EntityType<?> getEntityType()
  {
    return entityType;
  }

  MqlAstNode getAliasNode()
  {
    return aliasNode;
  }

  public String getAlias()
  {
    return alias;
  }

  public MqlAstNode getQueryNode()
  {
    return queryNode;
  }

  public Optional<EntityType<?>> getEntityTypeByAlias(String alias)
  {
    EntityType<?> entityType = null;
    for(
      var subQueryContext = this; (null == entityType
        && null != subQueryContext); subQueryContext = subQueryContext.getParent()
    )
    {
      entityType = subQueryContext.entityTypeByAliasMap.get(alias);
    }

    return Optional.ofNullable(entityType);
  }

  void putEntityTypeByAlias(String alias, EntityType<?> entityType)
  {
    if(entityTypeByAliasMap.containsKey(alias))
    {
      throw new MqlSyntaxException(String.format("Alias %s is duplicated", alias));
    }

    entityTypeByAliasMap.put(alias, entityType);
  }

  public Class<?> findJavaTypeOf(JpaEntityAttributePath path)
  {
    final var metamodel = queryBuilderContext.getMetamodel();

    Class<?> javaType = Object.class;
    EntityType<?> entityType = null;
    Attribute<?, ?> attr = null;
    var tokenIndex = 0;
    do
    {
      final var token = path.get(tokenIndex);

      if(0 == tokenIndex)
      {
        entityType = getEntityTypeByAlias(token)
          .orElseThrow(() -> new MqlSyntaxException(String.format(
            "Cannot determine the entity type of alias %s",
            token)));
      }
      else
      {
        try
        {
          if(1 == tokenIndex)
          {
            attr = entityType.getAttribute(token);
          }
          else
          {
            final var attrJavaType = attr.getJavaType();
            final var managedType = metamodel.managedType(attrJavaType);
            attr = managedType.getAttribute(token);
          }
        }
        catch(final IllegalArgumentException iae)
        {
          throw new MqlSyntaxException(iae.getMessage());
        }
      }

      ++tokenIndex;
    }
    while(tokenIndex < path.size());

    if(null == attr)
    {
      throw new MqlSyntaxException(String.format(
        "Cannot determine the java type of attribute path %s",
        path.toString()));
    }
    javaType = attr.getJavaType();

    return javaType;
  }

  private final MqlQueryBuilderContext queryBuilderContext;

  private MqlSubQueryContext parent;

  private final List<MqlSubQueryContext> children;

  private final MqlAstNode subQueryRootNode;

  private final MqlAstNode entityNameNode;

  private final EntityType<?> entityType;

  private final MqlAstNode aliasNode;

  private final String alias;

  private final MqlAstNode queryNode;

  private final Map<String, EntityType<?>> entityTypeByAliasMap;
}

package kaphein.jpa.mql;

import java.util.HashMap;
import java.util.Map;

import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.Metamodel;

class MqlQueryBuilderContext
{
  MqlQueryBuilderContext(
    Metamodel metamodel,
    MqlAstNode rootNode
  )
  {
    this.metamodel = metamodel;
    entityTypeByEntityNameMap = new HashMap<>();
    mqlParameterMap = new MqlParameterMap();
    this.rootNode = rootNode;
  }

  public Metamodel getMetamodel()
  {
    return metamodel;
  }

  public Map<String, EntityType<?>> getEntityTypeByEntityNameMap()
  {
    return entityTypeByEntityNameMap;
  }

  public MqlParameterMap getMqlParameterMap()
  {
    return mqlParameterMap;
  }

  public MqlAstNode getRootNode()
  {
    return rootNode;
  }

  private final Metamodel metamodel;

  private final Map<String, EntityType<?>> entityTypeByEntityNameMap;

  private final MqlParameterMap mqlParameterMap;

  private final MqlAstNode rootNode;
}

package kaphein.jpa.core;

import java.util.NoSuchElementException;
import java.util.Optional;

import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.Metamodel;

public class MetamodelUtils
{
  public static final Optional<EntityType<?>> findEntityTypeByEntityName(
    Metamodel metamodel,
    String entityName
  )
  {
    return metamodel
      .getEntities()
      .stream()
      .filter(e -> e.getName().equals(entityName))
      .findFirst();
  }

  public static final EntityType<?> findEntityTypeByEntityNameOrThrow(
    Metamodel metamodel,
    String entityName
  )
  {
    return findEntityTypeByEntityName(
      metamodel,
      entityName)
      .orElseThrow(() -> new NoSuchElementException(
        "Failed to find the entity type of " + entityName));
  }

  public static final <E> Optional<EntityType<E>> findEntityTypeByJavaType(
    Metamodel metamodel,
    Class<E> javaType
  )
  {
    EntityType<E> entityType = null;
    try
    {
      entityType = findEntityTypeByJavaTypeOrThrow(
        metamodel,
        javaType);
    }
    catch(final EntityTypeNotFoundException etnfe)
    {
      // Ignore the exception.
    }

    return Optional.ofNullable(entityType);
  }

  public static final <E> EntityType<E> findEntityTypeByJavaTypeOrThrow(
    Metamodel metamodel,
    Class<E> javaType
  )
  {
    EntityType<E> entityType = null;
    try
    {
      entityType = metamodel.entity(javaType);
    }
    catch(final IllegalArgumentException iae)
    {
      throw new EntityTypeNotFoundException(
        String.format("Cannot find an entity type for java type %s", javaType.getName()),
        iae);
    }

    return entityType;
  }

  public MetamodelUtils()
  {
    // Does nothing.
  }
}

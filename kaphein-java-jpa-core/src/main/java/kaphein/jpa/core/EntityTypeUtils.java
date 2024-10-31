package kaphein.jpa.core;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.Type;

public final class EntityTypeUtils
{
  public static <X> Set<? extends SingularAttribute<? super X, ?>> findIdAttributes(EntityType<X> entityType)
  {
    AssertArg.isNotNull(entityType, "entityType");

    final Set<SingularAttribute<? super X, ?>> idAttributes = new LinkedHashSet<>();
    if(entityType.hasSingleIdAttribute())
    {
      final Type<?> idType = entityType.getIdType();
      final SingularAttribute<? super X, ?> idAttribute = entityType.getId(idType.getJavaType());
      idAttributes.add(idAttribute);
    }
    else
    {
      final Set<SingularAttribute<? super X, ?>> idClassAttributes = entityType.getIdClassAttributes();
      idAttributes.addAll(idClassAttributes);
    }

    return Collections.unmodifiableSet(idAttributes);
  }

  /**
   * Does nothing, but allows DI frameworks (like Spring Framework) to declare
   * this class as a bean.
   */
  public EntityTypeUtils()
  {
    // Does nothing.
  }
}

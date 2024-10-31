package kaphein.jpa.mql;

import jakarta.persistence.TypedQuery;

public class MqlQueryBuildResult<E>
{
  MqlQueryBuildResult(
    TypedQuery<? extends E> itemQuery,
    TypedQuery<Long> countQuery
  )
  {
    this.itemQuery = itemQuery;
    this.countQuery = countQuery;
  }

  public TypedQuery<? extends E> getItemQuery()
  {
    return itemQuery;
  }

  public TypedQuery<Long> getCountQuery()
  {
    return countQuery;
  }

  private final TypedQuery<? extends E> itemQuery;

  private final TypedQuery<Long> countQuery;
}

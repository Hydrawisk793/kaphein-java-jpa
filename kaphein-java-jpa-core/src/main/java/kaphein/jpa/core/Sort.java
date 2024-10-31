package kaphein.jpa.core;

import java.io.Serializable;

public class Sort
{
  public static enum NullOrder
  {
    /**
     * Uses DBMS default policy for sorting {@code NULL} values.
     */
    DEFAULT,

    /**
     * {@code NULL}s are come first.
     */
    FIRST,

    /**
     * {@code NULL}s are come last.
     */
    LAST;
  }

  public static class Order implements Serializable
  {
    public Order(Order src)
    {
      this(
        src.getPath(),
        src.isDescending(),
        src.getNullOrder());
    }

    public Order(String attributePath)
    {
      this(
        new JpaEntityAttributePath(attributePath),
        false,
        NullOrder.DEFAULT);
    }

    public Order(JpaEntityAttributePath attributePath)
    {
      this(
        attributePath,
        false,
        NullOrder.DEFAULT);
    }

    public Order(
      String attributePath,
      boolean descending
    )
    {
      this(
        new JpaEntityAttributePath(attributePath),
        descending,
        NullOrder.DEFAULT);
    }

    public Order(
      JpaEntityAttributePath attributePath,
      boolean descending
    )
    {
      this(
        attributePath,
        descending,
        NullOrder.DEFAULT);
    }

    public Order(
      String attributePath,
      boolean descending,
      NullOrder nullOrder
    )
    {
      this(
        new JpaEntityAttributePath(attributePath),
        descending,
        nullOrder);
    }

    public Order(
      JpaEntityAttributePath attributePath,
      boolean descending,
      NullOrder nullOrder
    )
    {
      this.path = AssertArg.isNotNull(attributePath, "attributePath");
      this.descending = descending;
      this.nullOrder = AssertArg.isNotNull(nullOrder, "nullOrder");
    }

    public JpaEntityAttributePath getPath()
    {
      return path;
    }

    public boolean isDescending()
    {
      return descending;
    }

    public NullOrder getNullOrder()
    {
      return nullOrder;
    }

    private static final long serialVersionUID = 5968447222978642434L;

    private final JpaEntityAttributePath path;

    private final boolean descending;

    private final NullOrder nullOrder;
  }
}

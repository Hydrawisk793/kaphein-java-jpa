package kaphein.jpa.core;

import jakarta.persistence.metamodel.ManagedType;

public class InvalidAttributePathException extends RuntimeException
{
  public InvalidAttributePathException(
    ManagedType<?> type,
    String path
  )
  {
    super(String.format("Path %s does not exist in %s", path, type.getJavaType().getName()));
    this.type = AssertArg.isNotNull(type, "type");
    this.path = AssertArg.isNotNull(path, "path");
  }

  public ManagedType<?> getType()
  {
    return type;
  }

  public String getPath()
  {
    return path;
  }

  private static final long serialVersionUID = -459924833466399236L;

  private final ManagedType<?> type;

  private final String path;
}

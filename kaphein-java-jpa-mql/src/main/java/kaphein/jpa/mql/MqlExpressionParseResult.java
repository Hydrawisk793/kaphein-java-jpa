package kaphein.jpa.mql;

class MqlExpressionParseResult
{
  MqlExpressionParseResult(
    boolean completed,
    MqlAstNode rootNode
  )
  {
    this.completed = completed;
    this.rootNode = rootNode;
  }

  public boolean isCompleted()
  {
    return completed;
  }

  public MqlAstNode getRootNode()
  {
    return rootNode;
  }

  private final boolean completed;

  private final MqlAstNode rootNode;
}

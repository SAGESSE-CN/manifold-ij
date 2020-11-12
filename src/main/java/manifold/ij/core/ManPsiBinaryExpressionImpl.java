package manifold.ij.core;

import com.intellij.openapi.util.NlsSafe;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.tree.ChildRole;
import com.intellij.psi.impl.source.tree.JavaElementType;
import com.intellij.psi.impl.source.tree.java.PsiBinaryExpressionImpl;
import com.intellij.psi.impl.source.tree.java.PsiJavaTokenImpl;
import com.intellij.psi.tree.IElementType;
import com.intellij.util.IncorrectOperationException;
import manifold.ij.extensions.ManJavaResolveCache;
import org.jetbrains.annotations.NotNull;

// Supports binding expressions and binary operator overloading
public class ManPsiBinaryExpressionImpl extends PsiBinaryExpressionImpl implements IManOperatorOverloadReference
{
  public ManPsiBinaryExpressionImpl()
  {
    this( JavaElementType.BINARY_EXPRESSION );
  }

  protected ManPsiBinaryExpressionImpl( @NotNull IElementType elementType )
  {
    super( elementType );
  }

  @Override
  @NotNull
  public PsiJavaToken getOperationSign()
  {
    PsiJavaToken child = (PsiJavaToken)findChildByRoleAsPsiElement( ChildRole.OPERATION_SIGN );
    if( child == null )
    {
      // pose as multiplication to get by
      child = new PsiJavaTokenImpl( JavaTokenType.ASTERISK, "*" );
    }
    return child;
  }

  @Override
  public boolean isOverloaded()
  {
    if( getROperand() == null )
    {
      return false;
    }
    PsiMethod method = ManJavaResolveCache.getBinaryOperatorMethod(
      getOperationSign(),
      getLOperand().getType(),
      getROperand().getType(), this );
    return method != null;
  }

  public PsiReference getReference() {
    return isOverloaded() ? this : super.getReference();
  }

  @NotNull
  @Override
  public PsiElement getElement() {
    return this;
  }

  @NotNull
  @Override
  public TextRange getRangeInElement()
  {
    return TextRange.EMPTY_RANGE;
  }

  @Override
  public PsiElement resolve() {
    if( getROperand() == null )
    {
      return null;
    }
    PsiMethod method = ManJavaResolveCache.getBinaryOperatorMethod(
      getOperationSign(),
      getLOperand().getType(),
      getROperand().getType(), this );
    return method;
  }

  @NotNull
  @Override
  public @NlsSafe String getCanonicalText()
  {
    return "";
  }

  @Override
  public PsiElement handleElementRename( @NotNull String newElementName ) throws IncorrectOperationException
  {
    return null;
  }

  @Override
  public PsiElement bindToElement( @NotNull PsiElement element ) throws IncorrectOperationException
  {
    return null;
  }

  @Override
  public boolean isReferenceTo( @NotNull PsiElement element )
  {
    return resolve() == element;
  }

  @Override
  public boolean isSoft() {
    return false;
  }

}

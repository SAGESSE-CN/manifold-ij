/*
 *
 *  * Copyright (c) 2022 - Manifold Systems LLC
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *       http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 *
 */

package manifold.ij.extensions;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.impl.java.stubs.JavaLiteralExpressionElementType;
import com.intellij.psi.impl.java.stubs.impl.PsiLiteralStub;
import org.jetbrains.annotations.NotNull;

public class ManJavaLiteralExpressionElementType extends JavaLiteralExpressionElementType
{
  public PsiLiteralExpression createPsi( @NotNull ASTNode node )
  {
    return new ManPsiLiteralExpressionImpl( node );
  }

  @Override
  public PsiLiteralExpression createPsi( @NotNull PsiLiteralStub stub )
  {
    return new ManPsiLiteralExpressionImpl( stub );
  }
}

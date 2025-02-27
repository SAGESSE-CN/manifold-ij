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

import com.intellij.ide.highlighter.JavaFileHighlighter;
import com.intellij.lang.java.JavaParserDefinition;
import com.intellij.lexer.HtmlHighlightingLexer;
import com.intellij.lexer.LayeredLexer;
import com.intellij.lexer.Lexer;
import com.intellij.lexer.StringLiteralLexer;
import com.intellij.pom.java.LanguageLevel;
import com.intellij.psi.JavaDocTokenType;
import com.intellij.psi.JavaTokenType;
import com.intellij.psi.impl.source.tree.JavaDocElementType;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;

//!!
//!! Copied from IJ's JavaHighlightingLexer so we can handle '$' as a legal escape char (for string literal templates)
//!!
public class ManJavaFileHighlighter extends JavaFileHighlighter
{
  public ManJavaFileHighlighter( LanguageLevel languageLevel )
  {
    super( languageLevel );
  }

  @Override
  @NotNull
  public Lexer getHighlightingLexer()
  {
    return new ManJavaHighlightingLexer( myLanguageLevel );
  }

  private class ManJavaHighlightingLexer extends LayeredLexer
  {
    public ManJavaHighlightingLexer( @NotNull LanguageLevel languageLevel )
    {
      super( JavaParserDefinition.createLexer( languageLevel ) );
      
      //!!
      //!! This is where we add '$' to override IJ's default behavior of highlighting it as an illegal escape char
      //!!
      registerSelfStoppingLayer( new StringLiteralLexer( '\"', JavaTokenType.STRING_LITERAL, false, "$" ),
        new IElementType[]{JavaTokenType.STRING_LITERAL}, IElementType.EMPTY_ARRAY );


      //!! The rest is copied from IJ's JavaHighlightingLexer...

      registerSelfStoppingLayer( new StringLiteralLexer( '\'', JavaTokenType.STRING_LITERAL ),
        new IElementType[]{JavaTokenType.CHARACTER_LITERAL}, IElementType.EMPTY_ARRAY );

      LayeredLexer docLexer = new LayeredLexer( JavaParserDefinition.createDocLexer( languageLevel ) );
      HtmlHighlightingLexer htmlLexer = new HtmlHighlightingLexer( null );
      htmlLexer.setHasNoEmbeddments( true );
      docLexer.registerLayer( htmlLexer, JavaDocTokenType.DOC_COMMENT_DATA );
      registerSelfStoppingLayer( docLexer, new IElementType[]{JavaDocElementType.DOC_COMMENT}, IElementType.EMPTY_ARRAY );
    }
  }
}

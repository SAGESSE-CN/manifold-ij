package manifold.ij.extensions;

import com.intellij.lang.java.JavaLanguage;
import com.intellij.lang.jvm.annotation.JvmAnnotationArrayValue;
import com.intellij.lang.jvm.annotation.JvmAnnotationAttribute;
import com.intellij.lang.jvm.annotation.JvmAnnotationAttributeValue;
import com.intellij.lang.jvm.annotation.JvmNestedAnnotationValue;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.Key;
import com.intellij.psi.*;
import com.intellij.psi.augment.PsiAugmentProvider;
import com.intellij.psi.impl.compiled.ClsClassImpl;
import com.intellij.psi.impl.source.PsiExtensibleClass;
import manifold.ext.props.rt.api.propgen;
import manifold.ij.core.ManModule;
import manifold.ij.core.ManProject;
import manifold.ij.psi.ManLightFieldBuilder;
import manifold.ij.psi.ManLightModifierListImpl;
import manifold.ij.psi.ManPsiElementFactory;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static manifold.ij.extensions.PropertyMaker.generateAccessors;

/**
 * - Re-create non-backing property fields (from .class files compiled with declared properties).
 * - Generate getter/setter methods (for source files with declared properties)
 * - Create inferred property fields (for both .class and source files)
 */
public class ManPropertiesAugmentProvider extends PsiAugmentProvider
{
  public static final Key<Boolean> ACCESSOR_TAG = Key.create( "ACCESSOR_TAG" );
  public static final Key<SmartPsiElementPointer<PsiMethod>> GETTER_TAG = Key.create( "GETTER_TAG" );
  public static final Key<SmartPsiElementPointer<PsiMethod>> SETTER_TAG = Key.create( "SETTER_TAG" );

  @SuppressWarnings( "deprecation" )
  @NotNull
  public <E extends PsiElement> List<E> getAugments( @NotNull PsiElement element, @NotNull Class<E> cls )
  {
    return getAugments( element, cls, null );
  }

  @NotNull
  public <E extends PsiElement> List<E> getAugments( @NotNull PsiElement element, @NotNull Class<E> cls, String nameHint )
  {
    if( !ManProject.isManifoldInUse( element ) )
    {
      // Manifold jars are not used in the project
      return Collections.emptyList();
    }

    ManModule module = ManProject.getModule( element );
    if( module != null && !module.isPropertiesEnabled() )
    {
      // project/module not using properties
      return Collections.emptyList();
    }

    return ApplicationManager.getApplication().runReadAction( (Computable<List<E>>)() -> _getAugments( element, cls ) );
  }

  private <E extends PsiElement> List<E> _getAugments( PsiElement element, Class<E> cls )
  {
    // Module is assigned to user-data via ManTypeFinder, which loads the psiClass (element)
    if( DumbService.getInstance( element.getProject() ).isDumb() )
    {
      // skip processing during index rebuild
      return Collections.emptyList();
    }

    if( !(element instanceof PsiExtensibleClass) || !element.isValid() )
    {
      return Collections.emptyList();
    }

    PsiExtensibleClass psiClass = (PsiExtensibleClass)element;
    String className = psiClass.getQualifiedName();
    if( className == null )
    {
      return Collections.emptyList();
    }

    LinkedHashMap<String, PsiMember> augFeatures = new LinkedHashMap<>();

    if( PsiMethod.class.isAssignableFrom( cls ) )
    {
      addMethods( psiClass, augFeatures );
    }
    else if( PsiField.class.isAssignableFrom( cls ) )
    {
      recreateNonbackingPropertyFields( psiClass, augFeatures );
      inferPropertyFieldsFromAccessors( psiClass, augFeatures );
    }

    //noinspection unchecked
    return new ArrayList<>( (Collection<? extends E>)augFeatures.values() );
  }

  private void inferPropertyFieldsFromAccessors( PsiExtensibleClass psiClass, LinkedHashMap<String, PsiMember> augFeatures )
  {
    forceAncestryToAugment( psiClass );
    PropertyInference.inferPropertyFields( psiClass, augFeatures );
  }

  private static final Key<Boolean> forceAncestryToAugment_TAG = Key.create( "forceAncestryToAugment_TAG" );
  private void forceAncestryToAugment( PsiClass psiClass )
  {
    if( !(psiClass instanceof PsiExtensibleClass) || psiClass.getUserData( forceAncestryToAugment_TAG ) != null )
    {
      return;
    }

    psiClass.putUserData( forceAncestryToAugment_TAG, true );

    PsiClass st = psiClass.getSuperClass();
    forceAncestryToAugment( st );
    for( PsiClass iface : psiClass.getInterfaces() )
    {
      forceAncestryToAugment( iface );
    }
    // force augments to load on fields
    psiClass.getFields();
  }

  private void recreateNonbackingPropertyFields( PsiExtensibleClass psiClass, LinkedHashMap<String, PsiMember> augFeatures )
  {
    if( !(psiClass instanceof ClsClassImpl) )
    {
      return;
    }

    ClsClassImpl clsClass = (ClsClassImpl)psiClass;

    // Recreate non-backing property fields based on @propgen annotations on corresponding getter/setter
    //
    for( PsiMethod m : clsClass.getOwnMethods() )
    {
      PsiAnnotation propgenAnno = m.getAnnotation( propgen.class.getTypeName() );
      if( propgenAnno != null )
      {
        //noinspection ConstantConditions
        String fieldName = (String)((PsiLiteralValue)propgenAnno.findAttributeValue( "name" )).getValue();
        //noinspection ConstantConditions
        if( augFeatures.containsKey( fieldName ) ||
          clsClass.getOwnFields().stream()
            .anyMatch( f -> fieldName.equals( f.getName() ) ) )
        {
          // prop field already exists
          continue;
        }

        ManLightFieldBuilder propField = addPropField( psiClass, m, propgenAnno, fieldName );
        augFeatures.put( fieldName, propField );
      }
    }
  }

  private ManLightFieldBuilder addPropField( PsiExtensibleClass psiClass, PsiMethod m, PsiAnnotation propgenAnno, String fieldName )
  {
    @NotNull PsiParameter[] parameters = m.getParameterList().getParameters();
    PsiType type = parameters.length == 0 ? m.getReturnType() : parameters[0].getType();

    ManPsiElementFactory factory = ManPsiElementFactory.instance();
    ManLightFieldBuilder propField = factory.createLightField( psiClass.getManager(), fieldName, type )
      .withContainingClass( psiClass )
      .withNavigationElement( m );

    //noinspection ConstantConditions
    long flags = (long)((PsiLiteralValue)propgenAnno.findAttributeValue( "flags" )).getValue();
    List<String> modifiers = new ArrayList<>();
    for( ModifierMap modifier : ModifierMap.values() )
    {
      if( (flags & modifier.getMod()) != 0 )
      {
        modifiers.add( modifier.getName() );
      }
    }
    ManLightModifierListImpl modifierList = new ManLightModifierListImpl( psiClass.getManager(), JavaLanguage.INSTANCE,
      modifiers.toArray( new String[0] ) );
    propField.withModifierList( modifierList );

    // add the @var, @val, @get, @set, etc. annotations
    for( JvmAnnotationAttribute attr : propgenAnno.getAttributes() )
    {
      JvmAnnotationAttributeValue value = attr.getAttributeValue();
      if( value instanceof JvmAnnotationArrayValue )
      {
        List<JvmAnnotationAttributeValue> values = ((JvmAnnotationArrayValue)value).getValues();
        if( !values.isEmpty() )
        {
          //noinspection ConstantConditions
          String anno = ((PsiNameValuePair)attr).getValue().getText();
          anno = anno.substring( 1, anno.length() - 1 );
          //noinspection UnstableApiUsage
          String fqn = ((JvmNestedAnnotationValue)values.get( 0 )).getValue().getQualifiedName();
          modifierList.addRawAnnotation( fqn, anno );
        }
      }
    }
    return propField;
  }

  private void addMethods( PsiExtensibleClass psiClass, LinkedHashMap<String, PsiMember> augFeatures )
  {
    if( psiClass instanceof ClsClassImpl )
    {
      // .class files already have getter/setter methods
      return;
    }

    for( PsiField field : psiClass.getOwnFields() )
    {
      generateAccessors( field, psiClass, augFeatures );
    }
  }
}

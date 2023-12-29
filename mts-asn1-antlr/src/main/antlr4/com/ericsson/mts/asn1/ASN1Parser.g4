parser grammar ASN1Parser;

options { tokenVocab=ASN1Lexer; }

modules: moduleDefinition+;

moduleDefinition :  IDENTIFIER (L_BRACE (IDENTIFIER L_PARAN NUMBER R_PARAN)* R_BRACE)?
     DEFINITIONS_LITERAL
     tagDefault
     extensionDefault
     ASSIGN_OP
      BEGIN_LITERAL
     moduleBody
      END_LITERAL
        ;

tagDefault : ( (EXPLICIT_LITERAL|IMPLICIT_LITERAL|AUTOMATIC_LITERAL) TAGS_LITERAL )?
;

extensionDefault :
   (EXTENSIBILITY_LITERAL IMPLIED_LITERAL)?
;

moduleBody :  (exports imports assignmentList) ?
;
exports :   (EXPORTS_LITERAL symbolsExported SEMI_COLON
 |    EXPORTS_LITERAL ALL_LITERAL SEMI_COLON )?
;

symbolsExported : ( symbolList )?
;

imports :   (IMPORTS_LITERAL symbolsImported SEMI_COLON )?
;

symbolsImported : (symbolsFromModuleList )?
;

symbolsFromModuleList :
     (symbolsFromModule) (symbolsFromModule)*
;

symbolsFromModule : symbolList FROM_LITERAL globalModuleReference
;

globalModuleReference : IDENTIFIER assignedIdentifier
;

assignedIdentifier :
;

symbolList   : (symbol) (COMMA symbol)*
;

symbol  : IDENTIFIER ((L_BRACE  R_BRACE))?
;

//parameterizedReference :
//  reference (L_BRACE  R_BRACE)?
//;

//reference :
// IDENTIFIER |
//  identifier
//;

assignmentList :  (assignment) (assignment)*
;


assignment :
 (IDENTIFIER
	( objectAssignment
	 | valueAssignment
	 | typeAssignment
	 | parameterizedAssignment
	 | objectClassAssignment
	)
 )
	;

objectAssignment : definedObjectClass ASSIGN_OP object
;

sequenceType :SEQUENCE_LITERAL L_BRACE (extensionAndException  optionalExtensionMarker | componentTypeLists )? R_BRACE
	;
extensionAndException :  ELLIPSIS  exceptionSpec?
;
optionalExtensionMarker :  ( COMMA  ELLIPSIS )?
;

componentTypeLists :
   rootComponentTypeList (COMMA  extensionAndException  extensionAdditions   (optionalExtensionMarker|(COMMA  ELLIPSIS  COMMA  rootComponentTypeList)))?
//  |  rootComponentTypeList  COMMA  extensionAndException  extensionAdditions    optionalExtensionMarker
//  |  rootComponentTypeList  COMMA  extensionAndException  extensionAdditions     COMMA  ELLIPSIS  COMMA  rootComponentTypeList
  |  extensionAndException  extensionAdditions  (optionalExtensionMarker | (COMMA  ELLIPSIS  COMMA    rootComponentTypeList))
//  |  extensionAndException  extensionAdditions  optionalExtensionMarker
;
rootComponentTypeList  : componentTypeList
;
componentTypeList  : (componentType) (COMMA componentType)*
;
componentType  :
  namedType (OPTIONAL_LITERAL | DEFAULT_LITERAL value )?
 |  COMPONENTS_LITERAL OF_LITERAL  asnType
;

extensionAdditions  :  (COMMA  extensionAdditionList)?
;
extensionAdditionList  :  (extensionAddition) (COMMA  extensionAddition)*
;
extensionAddition  : componentType  |  extensionAdditionGroup
;
extensionAdditionGroup  :  DOUBLE_L_BRACKET  versionNumber  componentTypeList  DOUBLE_R_BRACKET
;
versionNumber  :  (NUMBER  COLON )?
;

sequenceOfType  : SEQUENCE_LITERAL (L_PARAN (constraint | sizeConstraint) R_PARAN)? OF_LITERAL (asnType | namedType )
;
sizeConstraint : SIZE_LITERAL constraint
	;

parameterizedAssignment :
 parameterList
(ASSIGN_OP
	(asnType
		|	value
		|	valueSet
	)
)
|( definedObjectClass ASSIGN_OP
	( object
		|	objectClass
		|	objectSet
	)

)
// parameterizedTypeAssignment
//| parameterizedValueAssignment
//| parameterizedValueSetTypeAssignment
//| parameterizedObjectClassAssignment
//| parameterizedObjectAssignment
//| parameterizedObjectSetAssignment
;
parameterList : L_BRACE parameter (COMMA parameter)* R_BRACE
;
parameter : (paramGovernor COLON)? IDENTIFIER
;
paramGovernor : governor | IDENTIFIER
;
//dummyGovernor : dummyReference
//;

governor : asnType | definedObjectClass
;


objectClassAssignment : /*IDENTIFIER*/ ASSIGN_OP objectClass
;

objectClass : definedObjectClass | objectClassDefn /*| parameterizedObjectClass */
;
definedObjectClass :
	(IDENTIFIER DOT)? IDENTIFIER
	| TYPE_IDENTIFIER_LITERAL
	|  ABSTRACT_SYNTAX_LITERAL
;
usefulObjectClassReference :
   TYPE_IDENTIFIER_LITERAL
 |  ABSTRACT_SYNTAX_LITERAL
;

externalObjectClassReference : IDENTIFIER DOT IDENTIFIER
;

objectClassDefn : CLASS_LITERAL L_BRACE  fieldSpec (COMMA fieldSpec  )*  R_BRACE  withSyntaxSpec?
;
withSyntaxSpec : WITH_LITERAL SYNTAX_LITERAL syntaxList
;
syntaxList : L_BRACE tokenOrGroupSpec+ R_BRACE
;

tokenOrGroupSpec : requiredToken | optionalGroup
;

optionalGroup : L_BRACKET (tokenOrGroupSpec)+ R_BRACKET
;

requiredToken : literal | primitiveFieldName
;
literal : IDENTIFIER | COMMA
;
primitiveFieldName :
	AMPERSAND IDENTIFIER;


fieldSpec :
	AMPERSAND IDENTIFIER
	(
	  typeOptionalitySpec?
  	| asnType (valueSetOptionalitySpec?  | UNIQUE_LITERAL? valueOptionalitySpec? )
	| fieldName (OPTIONAL_LITERAL | (DEFAULT_LITERAL (valueSet | value)))?
	| definedObjectClass (OPTIONAL_LITERAL | (DEFAULT_LITERAL (objectSet | object)))?

	)

//   typeFieldSpec
//  | fixedTypeValueFieldSpec
//  | variableTypeValueFieldSpec
//  | fixedTypeValueSetFieldSpec
//  | variableTypeValueSetFieldSpec
//  | objectFieldSpec
//  | objectSetFieldSpec
;

typeFieldSpec : AMPERSAND IDENTIFIER typeOptionalitySpec?
;
typeOptionalitySpec : OPTIONAL_LITERAL | (DEFAULT_LITERAL asnType)
;
fixedTypeValueFieldSpec : AMPERSAND IDENTIFIER asnType UNIQUE_LITERAL? valueOptionalitySpec ?
;
valueOptionalitySpec : OPTIONAL_LITERAL | (DEFAULT_LITERAL value)
;

variableTypeValueFieldSpec : AMPERSAND IDENTIFIER  fieldName valueOptionalitySpec ?
;

fixedTypeValueSetFieldSpec : AMPERSAND IDENTIFIER   asnType valueSetOptionalitySpec ?
;

valueSetOptionalitySpec : OPTIONAL_LITERAL | DEFAULT_LITERAL valueSet
;

object : definedObject | objectDefn /*| objectFromObject */|  parameterizedObject
;
parameterizedObject : definedObject actualParameterList
;


definedObject
	:	IDENTIFIER (DOT)?
	;

objectDefn : defaultSyntax | definedSyntax
;

defaultSyntax : L_BRACE (fieldSetting COMMA)* fieldSetting R_BRACE
;

fieldSetting : primitiveFieldName setting
;

setting :  asnType //object | /* objectSet */ | asnType  | value | valueSet
;

definedSyntax : L_BRACE (definedSyntaxToken)* R_BRACE
;

definedSyntaxToken : literal | setting
;

objectSet : L_BRACE objectSetSpec R_BRACE
;
objectSetSpec :
  rootElementSetSpec (COMMA ELLIPSIS (COMMA additionalElementSetSpec )?)?
 | ELLIPSIS (COMMA additionalElementSetSpec )?
;


fieldName :(AMPERSAND IDENTIFIER)(AMPERSAND IDENTIFIER DOT)*
;
valueSet : L_BRACE elementSetSpecs R_BRACE
;
elementSetSpecs :
 rootElementSetSpec (COMMA ELLIPSIS (COMMA additionalElementSetSpec)?)?
	;
rootElementSetSpec : elementSetSpec
;
additionalElementSetSpec : elementSetSpec
;
elementSetSpec : unions | ALL_LITERAL exclusions
;
unions :   (intersections) (unionMark intersections)*
;
exclusions : EXCEPT_LITERAL elements
;
intersections : (intersectionElements) (intersectionMark intersectionElements)*
;
unionMark  :  PIPE  |  UNION_LITERAL
;

intersectionMark  :  POWER |  INTERSECTION_LITERAL
;

elements  :
	objectSetElements
   |	subtypeElements
// |  L_PARAN elementSetSpec R_PARAN
;

objectSetElements :
    object /*| definedObject | objectSetFromObjects | parameterizedObjectSet      */
;


intersectionElements : elements (exclusions)?
;
subtypeElements :
  ((value | MIN_LITERAL) LESS_THAN?  DOUBLE_DOT LESS_THAN?  (value | MAX_LITERAL) ) // ValueRange
 |  sizeConstraint //SizeConstraint
 | (PATTERN_LITERAL value) //PatternConstraint
 | value //SingleValue
;


variableTypeValueSetFieldSpec : AMPERSAND IDENTIFIER    fieldName valueSetOptionalitySpec?
;
objectFieldSpec : AMPERSAND IDENTIFIER definedObjectClass objectOptionalitySpec?
;
objectOptionalitySpec : OPTIONAL_LITERAL | DEFAULT_LITERAL object
;
objectSetFieldSpec : AMPERSAND IDENTIFIER definedObjectClass objectSetOptionalitySpec ?
;
objectSetOptionalitySpec : OPTIONAL_LITERAL | DEFAULT_LITERAL objectSet
;

typeAssignment :
      ASSIGN_OP
      asnType
;

valueAssignment :
      asnType
	  ASSIGN_OP
       value
;
asnType : (builtinType | referencedType) (constraint)*
;
builtinType :
  bitStringType
 | characterStringType
 | choiceType
 | enumeratedType
 | integerType
 | sequenceType
 | sequenceOfType
 | setType
 | setOfType
 | objectidentifiertype
 | objectClassFieldType
 | octetStringType
 | realType
 | BOOLEAN_LITERAL
 | NULL_LITERAL
	;

objectClassFieldType : definedObjectClass DOT fieldName
;

realType : REAL_LITERAL
;

setType :  SET_LITERAL  L_BRACE  (extensionAndException  optionalExtensionMarker  | componentTypeLists)? R_BRACE
	;

setOfType    : SET_LITERAL (constraint | sizeConstraint)? OF_LITERAL (asnType | namedType)
;

referencedType :
  definedType
// | selectionType
// | typeFromObject
// | valueSetFromObjects
;
definedType :
IDENTIFIER (DOT IDENTIFIER)? actualParameterList?
;

constraint : L_PARAN constraintSpec  exceptionSpec? R_PARAN
//L_PARAN value DOT_DOT value R_PARAN
;

constraintSpec : generalConstraint | subtypeConstraint
;
userDefinedConstraint : CONSTRAINED_LITERAL BY_LITERAL L_BRACE userDefinedConstraintParameter (COMMA userDefinedConstraintParameter)* R_BRACE
;

generalConstraint :  userDefinedConstraint | tableConstraint | contentsConstraint
;
userDefinedConstraintParameter :
	governor (COLON
 		value
 		| valueSet
 		| object
 		| objectSet
 		)?
;

tableConstraint : /*simpleTableConstraint |*/ componentRelationConstraint
;
simpleTableConstraint : objectSet
;


contentsConstraint :
   CONTAINING_LITERAL asnType
 |  ENCODED_LITERAL BY_LITERAL value
 |  CONTAINING_LITERAL asnType ENCODED_LITERAL BY_LITERAL value
 |  WITH_LITERAL COMPONENTS_LITERAL L_BRACE componentPresenceLists R_BRACE
;

componentPresenceLists:
   componentPresenceList? (COMMA  ELLIPSIS (COMMA componentPresenceList)?)?
  |  ELLIPSIS (COMMA componentPresenceList)?
;

componentPresenceList: (componentPresence) (COMMA componentPresence)*
;

componentPresence: IDENTIFIER (ABSENT_LITERAL | PRESENT_LITERAL)
;


subtypeConstraint	:
elementSetSpecs
//((value | MIN_LITERAL) LESS_THAN? DOUBLE_DOT LESS_THAN?  (value | MAX_LITERAL) )
//	| sizeConstraint
//	| value
	;
value  :   builtinValue
;
builtinValue :
            integerValue
	|   realValue
        |   enumeratedValue
	|   choiceValue
	|   objectIdentifierValue
	|   octetStringValue
	|   booleanValue
 ;

objectIdentifierValue : L_BRACE  (objIdComponentsList|definedValue objIdComponentsList) R_BRACE
;

octetStringValue :
		CSTRING
	|	BSTRING
	|	CONTAINING_LITERAL value
;

objIdComponentsList
	: 	(objIdComponents) (objIdComponents)*
;
objIdComponents  :
			IDENTIFIER
	|    	numberForm
	|    	IDENTIFIER L_PARAN numberForm R_PARAN
	|    	definedValue
;

numberForm : NUMBER | definedValue
;

integerValue :  signedNumber
;

realValue : numericRealValue //| specialRealValue  | sequenceValue
;

numericRealValue : (MINUS)? realnumber
;

realnumber : NUMBER (DOT (NUMBER))? (Exponent)?
;

choiceValue  :    IDENTIFIER COLON value
;
enumeratedValue  : IDENTIFIER
;

signedNumber :  MINUS? NUMBER
;
choiceType    : CHOICE_LITERAL L_BRACE alternativeTypeLists R_BRACE
;
alternativeTypeLists :   rootAlternativeTypeList (COMMA
   extensionAndException  extensionAdditionAlternatives  optionalExtensionMarker )?
	;
extensionAdditionAlternatives  : (COMMA  extensionAdditionAlternativesList )?
;
extensionAdditionAlternativesList  : (extensionAdditionAlternative) (COMMA  extensionAdditionAlternative)*
;
extensionAdditionAlternative  :  extensionAdditionAlternativesGroup | namedType
;
extensionAdditionAlternativesGroup  :  DOUBLE_L_BRACKET  versionNumber  alternativeTypeList  DOUBLE_R_BRACKET
;

rootAlternativeTypeList  : alternativeTypeList
;
alternativeTypeList : (namedType) (COMMA namedType)*
;
namedType : IDENTIFIER   asnType
;
enumeratedType : ENUMERATED_LITERAL L_BRACE enumerations R_BRACE
;
enumerations : rootEnumeration (COMMA   ELLIPSIS exceptionSpec? (COMMA   additionalEnumeration )?)?
	;
rootEnumeration : enumeration
;
enumeration : enumerationItem ( COMMA enumerationItem)*
;
enumerationItem : IDENTIFIER | namedNumber | value
;
namedNumber :   IDENTIFIER L_PARAN (signedNumber | definedValue) R_PARAN
;
definedValue :
 // externalValueReference
 //| valuereference
  parameterizedValue
;
parameterizedValue : simpleDefinedValue (actualParameterList)?
;
simpleDefinedValue : IDENTIFIER (DOT IDENTIFIER)?
;

actualParameterList : L_BRACE actualParameter (COMMA actualParameter)* R_BRACE
;
actualParameter : asnType | value /*| valueSet | definedObjectClass | object | objectSet*/
;
exceptionSpec : EXCLAM  exceptionIdentification
;
exceptionIdentification : signedNumber
 |     definedValue
 |     asnType COLON value
;
additionalEnumeration : enumeration
;
integerType:INTEGER_LITERAL  (L_BRACE namedNumberList R_BRACE)?
;
namedNumberList : (namedNumber) (COMMA namedNumber)*
;
objectidentifiertype  :  OBJECT_LITERAL IDENTIFIER_LITERAL
;
componentRelationConstraint : L_BRACE (IDENTIFIER (DOT IDENTIFIER)?) R_BRACE
			     (L_BRACE atNotation (COMMA atNotation)* R_BRACE)?
;
atNotation :  (A_ROND | (A_ROND_DOT level)) componentIdList
;
level : (DOT level)?
;

componentIdList : IDENTIFIER (DOT IDENTIFIER)*  //?????
;
octetStringType  :  OCTET_LITERAL STRING_LITERAL
;
bitStringType    : (BIT_LITERAL STRING_LITERAL) (L_BRACE namedBitList R_BRACE)?
;
namedBitList: (namedBit) (COMMA namedBit)*
;
namedBit      : IDENTIFIER L_PARAN (NUMBER | definedValue) R_PARAN
	;

booleanValue:  TRUE_LITERAL | FALSE_LITERAL | TRUE_SMALL_LITERAL | FALSE_SMALL_LITERAL
;

characterStringType: restrictedCharacterStringType
;

restrictedCharacterStringType: PRINTABLE_STRING;
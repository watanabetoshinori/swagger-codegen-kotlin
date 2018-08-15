package io.swagger.codegen.language

import com.google.common.base.Predicate
import com.google.common.collect.Iterators
import com.google.common.collect.Lists
import io.swagger.codegen.*

import io.swagger.models.Model
import io.swagger.models.ModelImpl
import io.swagger.models.Operation
import io.swagger.models.Swagger
import io.swagger.models.parameters.HeaderParameter
import io.swagger.models.parameters.Parameter
import io.swagger.models.properties.ArrayProperty
import io.swagger.models.properties.MapProperty
import io.swagger.models.properties.Property

import org.apache.commons.lang3.ArrayUtils
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.text.WordUtils

import java.io.File
import java.util.ArrayList
import java.util.Arrays
import java.util.HashMap
import java.util.HashSet
import java.util.regex.Matcher
import java.util.regex.Pattern

//
// This code was converted from the following java code :
// https://github.com/swagger-api/swagger-codegen/blob/master/modules/swagger-codegen/src/main/java/io/swagger/codegen/languages/Swift4Codegen.java
//

class Swift4Codegen : io.swagger.codegen.DefaultCodegen(), CodegenConfig {

    private var projectName = "SwaggerClient"

    private var unwrapRequired: Boolean = false

    private var objcCompatible = false

    private var lenientTypeCast = false

    private var swiftUseApiNamespace: Boolean = false

    private var responseAs = arrayOfNulls<String>(0)

    private var sourceFolder = "Classes" + File.separator + "Swaggers"

    override fun getTag(): CodegenType {
        return CodegenType.CLIENT
    }

    override fun getHelp(): String {
        return "swift4"
    }

    override fun getName(): String {
        return "Generates a swift client library."
    }

    override protected fun addAdditionPropertiesToCodeGenModel(codegenModel: CodegenModel, swaggerModel: ModelImpl) {
        val additionalProperties = swaggerModel.getAdditionalProperties()

        if (additionalProperties != null) {
            codegenModel.additionalPropertiesType = getSwaggerType(additionalProperties)
        }
    }

    /**
     * Constructor for the swift4 language codegen module.
     */
    init {
        outputFolder = "generated-code" + File.separator + "swift"
        modelTemplateFiles.put("model.mustache", ".swift")
        apiTemplateFiles.put("api.mustache", ".swift")
        templateDir = "swift4"
        embeddedTemplateDir = templateDir
        apiPackage = File.separator + "APIs"
        modelPackage = File.separator + "Models"

        languageSpecificPrimitives = hashSetOf(
            "Int",
            "Int32",
            "Int64",
            "Float",
            "Double",
            "Bool",
            "Void",
            "String",
            "Character",
            "AnyObject",
            "Any"
        )

        defaultIncludes = hashSetOf(
            "Data",
            "Date",
            "URL", // for file
            "UUID",
            "Array",
            "Dictionary",
            "Set",
            "Any",
            "Empty",
            "AnyObject",
            "Any"
        )

        reservedWords = hashSetOf(
            // name used by swift client
            "ErrorResponse", "Response",

            // Added for Objective-C compatibility
            "id", "description", "NSArray", "NSURL", "CGFloat", "NSSet", "NSString", "NSInteger", "NSUInteger",
            "NSError", "NSDictionary",

            //
            // Swift keywords. This list is taken from here:
            // https://developer.apple.com/library/content/documentation/Swift/Conceptual/Swift_Programming_Language/LexicalStructure.html#//apple_ref/doc/uid/TP40014097-CH30-ID410
            //
            // Keywords used in declarations
            "associatedtype", "class", "deinit", "enum", "extension", "fileprivate", "func", "import", "init",
            "inout", "internal", "let", "open", "operator", "private", "protocol", "public", "static", "struct",
            "subscript", "typealias", "var",
            // Keywords uses in statements
            "break", "case", "continue", "default", "defer", "do", "else", "fallthrough", "for", "guard", "if",
            "in", "repeat", "return", "switch", "where", "while",
            // Keywords used in expressions and types
            "as", "Any", "catch", "false", "is", "nil", "rethrows", "super", "self", "Self", "throw", "throws", "true", "try",
            // Keywords used in patterns
            "_",
            // Keywords that begin with a number sign
            "#available", "#colorLiteral", "#column", "#else", "#elseif", "#endif", "#file", "#fileLiteral", "#function", "#if",
            "#imageLiteral", "#line", "#selector", "#sourceLocation",
            // Keywords reserved in particular contexts
            "associativity", "convenience", "dynamic", "didSet", "final", "get", "infix", "indirect", "lazy", "left",
            "mutating", "none", "nonmutating", "optional", "override", "postfix", "precedence", "prefix", "Protocol",
            "required", "right", "set", "Type", "unowned", "weak", "willSet",

            //
            // Swift Standard Library types
            // https://developer.apple.com/documentation/swift
            //
            // Numbers and Basic Values
            "Bool", "Int", "Double", "Float", "Range", "ClosedRange", "Error", "Optional",
            // Special-Use Numeric Types
            "UInt", "UInt8", "UInt16", "UInt32", "UInt64", "Int8", "Int16", "Int32", "Int64", "Float80", "Float32", "Float64",
            // Strings and Text
            "String", "Character", "Unicode", "StaticString",
            // Collections
            "Array", "Dictionary", "Set", "OptionSet", "CountableRange", "CountableClosedRange",

            // The following are commonly-used Foundation types
            "URL", "Data", "Codable", "Encodable", "Decodable",

            // The following are other words we want to reserve
            "Void", "AnyObject", "Class", "dynamicType", "COLUMN", "FILE", "FUNCTION", "LINE"
        )

        typeMapping = HashMap()
        typeMapping.put("array", "Array")
        typeMapping.put("List", "Array")
        typeMapping.put("map", "Dictionary")
        typeMapping.put("date", "Date")
        typeMapping.put("Date", "Date")
        typeMapping.put("DateTime", "Date")
        typeMapping.put("boolean", "Bool")
        typeMapping.put("string", "String")
        typeMapping.put("char", "Character")
        typeMapping.put("short", "Int")
        typeMapping.put("int", "Int")
        typeMapping.put("long", "Int64")
        typeMapping.put("integer", "Int")
        typeMapping.put("Integer", "Int")
        typeMapping.put("float", "Float")
        typeMapping.put("number", "Double")
        typeMapping.put("double", "Double")
        typeMapping.put("object", "Any")
        typeMapping.put("file", "URL")
        typeMapping.put("binary", "Data")
        typeMapping.put("ByteArray", "Data")
        typeMapping.put("UUID", "UUID")

        importMapping = HashMap()

        cliOptions.add(CliOption(PROJECT_NAME, "Project name in Xcode"))
        cliOptions.add(CliOption(RESPONSE_AS,
                "Optionally use libraries to manage response.  Currently "
                        + StringUtils.join(RESPONSE_LIBRARIES, ", ")
                        + " are available."))
        cliOptions.add(CliOption(UNWRAP_REQUIRED,
                "Treat 'required' properties in response as non-optional "
                        + "(which would crash the app if api returns null as opposed "
                        + "to required option specified in json schema"))
        cliOptions.add(CliOption(OBJC_COMPATIBLE,
                "Add additional properties and methods for Objective-C " + "compatibility (default: false)"))
        cliOptions.add(CliOption(POD_SOURCE, "Source information used for Podspec"))
        cliOptions.add(CliOption(CodegenConstants.POD_VERSION, "Version used for Podspec"))
        cliOptions.add(CliOption(POD_AUTHORS, "Authors used for Podspec"))
        cliOptions.add(CliOption(POD_SOCIAL_MEDIA_URL, "Social Media URL used for Podspec"))
        cliOptions.add(CliOption(POD_DOCSET_URL, "Docset URL used for Podspec"))
        cliOptions.add(CliOption(POD_LICENSE, "License used for Podspec"))
        cliOptions.add(CliOption(POD_HOMEPAGE, "Homepage used for Podspec"))
        cliOptions.add(CliOption(POD_SUMMARY, "Summary used for Podspec"))
        cliOptions.add(CliOption(POD_DESCRIPTION, "Description used for Podspec"))
        cliOptions.add(CliOption(POD_SCREENSHOTS, "Screenshots used for Podspec"))
        cliOptions.add(CliOption(POD_DOCUMENTATION_URL,
                "Documentation URL used for Podspec"))
        cliOptions.add(CliOption(SWIFT_USE_API_NAMESPACE,
                "Flag to make all the API classes inner-class " + "of {{projectName}}API"))
        cliOptions.add(CliOption(CodegenConstants.HIDE_GENERATION_TIMESTAMP,
                CodegenConstants.HIDE_GENERATION_TIMESTAMP_DESC)
                .defaultValue("true"))
        cliOptions.add(CliOption(LENIENT_TYPE_CAST,
                "Accept and cast values for simple types (string->bool, " + "string->int, int->string)")
                .defaultValue("false"))
    }

    override fun processOpts() {
        super.processOpts()

        // Setup project name
        if (additionalProperties.containsKey(PROJECT_NAME)) {
            setProjectName(additionalProperties.get(PROJECT_NAME) as String)
        } else {
            additionalProperties.put(PROJECT_NAME, projectName)
        }
        sourceFolder = projectName + File.separator + sourceFolder

        // Setup unwrapRequired option, which makes all the
        // properties with "required" non-optional
        if (additionalProperties.containsKey(UNWRAP_REQUIRED)) {
            setUnwrapRequired(convertPropertyToBooleanAndWriteBack(UNWRAP_REQUIRED))
        }
        additionalProperties.put(UNWRAP_REQUIRED, unwrapRequired)

        // Setup objcCompatible option, which adds additional properties
        // and methods for Objective-C compatibility
        if (additionalProperties.containsKey(OBJC_COMPATIBLE)) {
            setObjcCompatible(convertPropertyToBooleanAndWriteBack(OBJC_COMPATIBLE))
        }
        additionalProperties.put(OBJC_COMPATIBLE, objcCompatible)

        // Setup unwrapRequired option, which makes all the properties with "required" non-optional
        if (additionalProperties.containsKey(RESPONSE_AS)) {
            val responseAsObject = additionalProperties.get(RESPONSE_AS)
            if (responseAsObject is String) {
                setResponseAs((responseAsObject as String).split(",") as Array<String>)
            } else {
                setResponseAs(responseAsObject as Array<String>)
            }
        }
        additionalProperties.put(RESPONSE_AS, responseAs)
        if (ArrayUtils.contains(responseAs, LIBRARY_PROMISE_KIT)) {
            additionalProperties.put("usePromiseKit", true)
        }
        if (ArrayUtils.contains(responseAs, LIBRARY_RX_SWIFT)) {
            additionalProperties.put("useRxSwift", true)
        }

        // Setup swiftUseApiNamespace option, which makes all the API
        // classes inner-class of {{projectName}}API
        if (additionalProperties.containsKey(SWIFT_USE_API_NAMESPACE)) {
            setSwiftUseApiNamespace(convertPropertyToBooleanAndWriteBack(SWIFT_USE_API_NAMESPACE))
        }

        if (!additionalProperties.containsKey(POD_AUTHORS)) {
            additionalProperties.put(POD_AUTHORS, DEFAULT_POD_AUTHORS)
        }

        setLenientTypeCast(convertPropertyToBooleanAndWriteBack(LENIENT_TYPE_CAST))

        supportingFiles.add(SupportingFile("Podspec.mustache",
                "",
                "$projectName.podspec"))
        supportingFiles.add(SupportingFile("Cartfile.mustache",
                "",
                "Cartfile"))
        supportingFiles.add(SupportingFile("APIHelper.mustache",
                sourceFolder,
                "APIHelper.swift"))
        supportingFiles.add(SupportingFile("AlamofireImplementations.mustache",
                sourceFolder,
                "AlamofireImplementations.swift"))
        supportingFiles.add(SupportingFile("Configuration.mustache",
                sourceFolder,
                "Configuration.swift"))
        supportingFiles.add(SupportingFile("Extensions.mustache",
                sourceFolder,
                "Extensions.swift"))
        supportingFiles.add(SupportingFile("Models.mustache",
                sourceFolder,
                "Models.swift"))
        supportingFiles.add(SupportingFile("APIs.mustache",
                sourceFolder,
                "APIs.swift"))
        supportingFiles.add(SupportingFile("CodableHelper.mustache",
                sourceFolder,
                "CodableHelper.swift"))
        supportingFiles.add(SupportingFile("JSONEncodableEncoding.mustache",
                sourceFolder,
                "JSONEncodableEncoding.swift"))
        supportingFiles.add(SupportingFile("JSONEncodingHelper.mustache",
                sourceFolder,
                "JSONEncodingHelper.swift"))
        supportingFiles.add(SupportingFile("git_push.sh.mustache",
                "",
                "git_push.sh"))
        supportingFiles.add(SupportingFile("gitignore.mustache",
                "",
                ".gitignore"))

    }

    override protected fun isReservedWord(word: String?): Boolean {
        return word != null && reservedWords.contains(word) //don't lowercase as super does
    }

    override fun escapeReservedWord(name: String): String {
        if (this.reservedWordsMappings().containsKey(name)) {
            return this.reservedWordsMappings().get(name)!!
        } else {
            // add an underscore to the name
            return "_$name"
        }
    }

    override fun modelFileFolder(): String {
        return (outputFolder + File.separator + sourceFolder
                + modelPackage().replace('.', File.separatorChar))
    }

    override fun apiFileFolder(): String {
        return (outputFolder + File.separator + sourceFolder
                + apiPackage().replace('.', File.separatorChar))
    }

    override fun getTypeDeclaration(prop: Property): String {
        if (prop is ArrayProperty) {
            val ap = prop as ArrayProperty
            val inner = ap.getItems()
            return "[" + getTypeDeclaration(inner) + "]"
        } else if (prop is MapProperty) {
            val mp = prop as MapProperty
            val inner = mp.getAdditionalProperties()
            return "[String:" + getTypeDeclaration(inner) + "]"
        }
        return super.getTypeDeclaration(prop)
    }

    override fun getSwaggerType(prop: Property): String {
        val swaggerType = super.getSwaggerType(prop)
        val type: String
        if (typeMapping.containsKey(swaggerType)) {
            type = typeMapping.get(swaggerType)!!
            if (languageSpecificPrimitives.contains(type) || defaultIncludes.contains(type)) {
                return type
            }
        } else {
            type = swaggerType
        }
        return toModelName(type)
    }

    override fun isDataTypeFile(dataType: String?): Boolean {
        return dataType != null && dataType.equals("URL")
    }

    override fun isDataTypeBinary(dataType: String?): Boolean {
        return dataType != null && dataType.equals("Data")
    }

    /**
     * Output the proper model name (capitalized).
     *
     * @param name the name of the model
     * @return capitalized model name
     */
    override fun toModelName(name: String): String {
        // FIXME parameter should not be assigned. Also declare it as "final"
        var name = sanitizeName(name)

        if (!StringUtils.isEmpty(modelNameSuffix)) { // set model suffix
            name = name + "_" + modelNameSuffix
        }

        if (!StringUtils.isEmpty(modelNamePrefix)) { // set model prefix
            name = modelNamePrefix + "_" + name
        }

        // camelize the model name
        // phone_number => PhoneNumber
        name = camelize(name)

        // model name cannot use reserved keyword, e.g. return
        if (isReservedWord(name)) {
            val modelName = "Model$name"
            LOGGER.warn(name + " (reserved word) cannot be used as model name. Renamed to "
                    + modelName)
            return modelName
        }

        // model name starts with number
        if (name.matches( Regex("^\\\\d.*"))) {
            // e.g. 200Response => Model200Response (after camelize)
            val modelName = "Model$name"
            LOGGER.warn(name
                    + " (model name starts with number) cannot be used as model name."
                    + " Renamed to " + modelName)
            return modelName
        }

        return name
    }

    /**
     * Return the capitalized file name of the model.
     *
     * @param name the model name
     * @return the file name of the model
     */
    override fun toModelFilename(name: String): String {
        // should be the same as the model name
        return toModelName(name)
    }

    override fun toDefaultValue(prop: Property): String? {
        // nil
        return null
    }

    override fun toInstantiationType(prop: Property): String? {
        if (prop is MapProperty) {
            val ap = prop as MapProperty
            return getSwaggerType(ap.getAdditionalProperties())
        } else if (prop is ArrayProperty) {
            val ap = prop as ArrayProperty
            val inner = getSwaggerType(ap.getItems())
            return "[$inner]"
        }
        return null
    }

    override fun toApiName(name: String): String {
        return if (name.length === 0) {
            "DefaultAPI"
        } else initialCaps(name) + "API"
    }

    override fun toOperationId(operationId: String): String {
        var operationId = camelize(sanitizeName(operationId), true)

        // Throw exception if method name is empty.
        // This should not happen but keep the check just in case
        if (StringUtils.isEmpty(operationId)) {
            throw RuntimeException("Empty method name (operationId) not allowed")
        }

        // method name cannot use reserved keyword, e.g. return
        if (isReservedWord(operationId)) {
            val newOperationId = camelize("call_$operationId", true)
            LOGGER.warn(operationId + " (reserved word) cannot be used as method name."
                    + " Renamed to " + newOperationId)
            return newOperationId
        }

        return operationId
    }

    override fun toVarName(name: String): String {
        // sanitize name
        var name = sanitizeName(name)

        // if it's all uppper case, do nothing
        if (name.matches(Regex("^[A-Z_]*$"))) {
            return name
        }

        // camelize the variable name
        // pet_id => petId
        name = camelize(name, true)

        // for reserved word or word starting with number, append _
        if (isReservedWord(name) || name.matches(Regex("^\\d.*"))) {
            name = escapeReservedWord(name)
        }

        return name
    }

    override fun toParamName(name: String): String {
        // sanitize name
        var name = sanitizeName(name)

        // replace - with _ e.g. created-at => created_at
        name = name.replace("-", "_")

        // if it's all uppper case, do nothing
        if (name.matches(Regex("^[A-Z_]*$"))) {
            return name
        }

        // camelize(lower) the variable name
        // pet_id => petId
        name = camelize(name, true)

        // for reserved word or word starting with number, append _
        if (isReservedWord(name) || name.matches(Regex("^\\d.*"))) {
            name = escapeReservedWord(name)
        }

        return name
    }

    override fun fromModel(name: String, model: Model, allDefinitions: Map<String, Model>?): CodegenModel {
        var codegenModel = super.fromModel(name, model, allDefinitions)
        if (codegenModel.description != null) {
            codegenModel.imports.add("ApiModel")
        }
        if (allDefinitions != null) {
            var parentSchema = codegenModel.parentSchema

            // multilevel inheritance: reconcile properties of all the parents
            while (parentSchema != null) {
                val parentModel = allDefinitions[parentSchema]
                val parentCodegenModel = super.fromModel(codegenModel.parent,
                        parentModel,
                        allDefinitions)
                codegenModel = Swift4Codegen.reconcileProperties(codegenModel, parentCodegenModel)

                // get the next parent
                parentSchema = parentCodegenModel.parentSchema
            }
        }

        return codegenModel
    }

    fun setProjectName(projectName: String) {
        this.projectName = projectName
    }

    fun setUnwrapRequired(unwrapRequired: Boolean) {
        this.unwrapRequired = unwrapRequired
    }

    fun setObjcCompatible(objcCompatible: Boolean) {
        this.objcCompatible = objcCompatible
    }

    fun setLenientTypeCast(lenientTypeCast: Boolean) {
        this.lenientTypeCast = lenientTypeCast
    }

    fun setResponseAs(responseAs: Array<String>) {
        this.responseAs = responseAs as Array<String?>
    }

    fun setSwiftUseApiNamespace(swiftUseApiNamespace: Boolean) {
        this.swiftUseApiNamespace = swiftUseApiNamespace
    }

    override fun toEnumValue(value: String, datatype: String): String {
        return value.toString()
    }

    override fun toEnumDefaultValue(value: String, datatype: String): String {
        return datatype + "_" + value
    }

    override fun toEnumVarName(name: String, datatype: String): String {
        var name = name
        if (name.length === 0) {
            return "empty"
        }

        val startWithNumberPattern = Pattern.compile("^\\d+")
        val startWithNumberMatcher = startWithNumberPattern.matcher(name)
        if (startWithNumberMatcher.find()) {
            val startingNumbers = startWithNumberMatcher.group(0)
            val nameWithoutStartingNumbers = name.substring(startingNumbers.length)

            return "_" + startingNumbers + camelize(nameWithoutStartingNumbers, true)
        }

        // for symbol, e.g. $, #
        if (getSymbolName(name) != null) {
            return camelize(WordUtils.capitalizeFully(getSymbolName(name).toUpperCase()), true)
        }

        // Camelize only when we have a structure defined below
        var camelized = false
        if (name.matches(Regex("[A-Z][a-z0-9]+[a-zA-Z0-9]*"))) {
            name = camelize(name, true)
            camelized = true
        }

        // Reserved Name
        val nameLowercase = StringUtils.lowerCase(name)
        if (isReservedWord(nameLowercase)) {
            return escapeReservedWord(nameLowercase)
        }

        // Check for numerical conversions
        if ("Int".equals(datatype) 
            || "Int32".equals(datatype) 
            || "Int64".equals(datatype) 
            || "Float".equals(datatype) 
            || "Double".equals(datatype)) {
            
            var varName = "number" + camelize(name)
            varName = varName.replace("-", "minus")
            varName = varName.replace("\\+", "plus")
            varName = varName.replace("\\.", "dot")
            return varName
        }

        // If we have already camelized the word, don't progress
        // any further
        if (camelized) {
            return name
        }

        return camelize(WordUtils.capitalizeFully(StringUtils.lowerCase(name), '-', '_', ' ', ':', '(', ')')
                .replace("[-_ :\\(\\)]", ""),
                true)
    }

    override fun toEnumName(property: CodegenProperty): String {
        var enumName = toModelName(property.name)

        // Ensure that the enum type doesn't match a reserved word or
        // the variable name doesn't match the generated enum type or the
        // Swift compiler will generate an error
        if (isReservedWord(property.datatypeWithEnum) || toVarName(property.name).equals(property.datatypeWithEnum)) {
            enumName = property.datatypeWithEnum + "Enum"
        }

        // TODO: toModelName already does something for names starting with number,
        // so this code is probably never called
        return if (enumName.matches(Regex("\\d.*"))) { // starts with number
            "_$enumName"
        } else {
            enumName
        }
    }

    override fun postProcessModels(objs: MutableMap<String, Any>?): MutableMap<String, Any> {
        val postProcessedModelsEnum = postProcessModelsEnum(objs)

        // We iterate through the list of models, and also iterate through each of the
        // properties for each model. For each property, if:
        //
        // CodegenProperty.name != CodegenProperty.baseName
        //
        // then we set
        //
        // CodegenProperty.vendorExtensions["x-codegen-escaped-property-name"] = true
        //
        // Also, if any property in the model has x-codegen-escaped-property-name=true, then we mark:
        //
        // CodegenModel.vendorExtensions["x-codegen-has-escaped-property-names"] = true
        //
        val models = postProcessedModelsEnum.get("models") as List<Object>
        for (_mo in models) {
            val mo = _mo as Map<String, Object>
            val cm = mo["model"] as CodegenModel
            var modelHasPropertyWithEscapedName = false
            for (prop in cm.allVars) {
                if (!prop.name.equals(prop.baseName)) {
                    prop.vendorExtensions.put("x-codegen-escaped-property-name", true)
                    modelHasPropertyWithEscapedName = true
                }
            }
            if (modelHasPropertyWithEscapedName) {
                cm.vendorExtensions.put("x-codegen-has-escaped-property-names", true)
            }
        }

        return postProcessedModelsEnum
    }

    override fun postProcessModelProperty(model: CodegenModel, property: CodegenProperty) {
        super.postProcessModelProperty(model, property)

        // The default template code has the following logic for
        // assigning a type as Swift Optional:
        //
        // {{^unwrapRequired}}?{{/unwrapRequired}}
        // {{#unwrapRequired}}{{^required}}?{{/required}}{{/unwrapRequired}}
        //
        // which means:
        //
        // boolean isSwiftOptional = !unwrapRequired || (unwrapRequired && !property.required);
        //
        // We can drop the check for unwrapRequired in (unwrapRequired && !property.required)
        // due to short-circuit evaluation of the || operator.
        val isSwiftOptional = !unwrapRequired || !property.required
        val isSwiftScalarType = (property.isInteger || property.isLong || property.isFloat || property.isDouble || property.isBoolean)
        if (isSwiftOptional && isSwiftScalarType) {
            // Optional scalar types like Int?, Int64?, Float?, Double?, and Bool?
            // do not translate to Objective-C. So we want to flag those
            // properties in case we want to put special code in the templates
            // which provide Objective-C compatibility.
            property.vendorExtensions.put("x-swift-optional-scalar", true)
        }
    }

    override fun escapeQuotationMark(input: String): String {
        // remove " to avoid code injection
        return input.replace("\"", "")
    }

    override fun escapeUnsafeCharacters(input: String): String {
        return input.replace("*/", "*_/").replace("/*", "/_*")
    }

    companion object {

        val PROJECT_NAME = "projectName"

        val RESPONSE_AS = "responseAs"

        val UNWRAP_REQUIRED = "unwrapRequired"

        val OBJC_COMPATIBLE = "objcCompatible"

        val POD_SOURCE = "podSource"

        val POD_AUTHORS = "podAuthors"

        val POD_SOCIAL_MEDIA_URL = "podSocialMediaURL"

        val POD_DOCSET_URL = "podDocsetURL"

        val POD_LICENSE = "podLicense"

        val POD_HOMEPAGE = "podHomepage"

        val POD_SUMMARY = "podSummary"

        val POD_DESCRIPTION = "podDescription"

        val POD_SCREENSHOTS = "podScreenshots"

        val POD_DOCUMENTATION_URL = "podDocumentationURL"

        val SWIFT_USE_API_NAMESPACE = "swiftUseApiNamespace"

        val DEFAULT_POD_AUTHORS = "Swagger Codegen"

        val LENIENT_TYPE_CAST = "lenientTypeCast"

        protected val LIBRARY_PROMISE_KIT = "PromiseKit"

        protected val LIBRARY_RX_SWIFT = "RxSwift"

        protected val RESPONSE_LIBRARIES = arrayOf(LIBRARY_PROMISE_KIT, LIBRARY_RX_SWIFT)

        private fun reconcileProperties(codegenModel: CodegenModel, parentCodegenModel: CodegenModel): CodegenModel {
            // To support inheritance in this generator, we will analyze
            // the parent and child models, look for properties that match, and remove
            // them from the child models and leave them in the parent.
            // Because the child models extend the parents, the properties
            // will be available via the parent.

            // Get the properties for the parent and child models
            val parentModelCodegenProperties = parentCodegenModel.vars
            val codegenProperties = codegenModel.vars
            codegenModel.allVars = ArrayList<CodegenProperty>(codegenProperties)
            codegenModel.parentVars = parentCodegenModel.allVars

            // Iterate over all of the parent model properties
            var removedChildProperty = false

            for (parentModelCodegenProperty in parentModelCodegenProperties) {
                // Now that we have found a prop in the parent class,
                // and search the child class for the same prop.
                val iterator = codegenProperties.iterator()
                while (iterator.hasNext()) {
                    val codegenProperty = iterator.next()
                    if (codegenProperty.baseName === parentModelCodegenProperty.baseName) {
                        // We found a property in the child class that is
                        // a duplicate of the one in the parent, so remove it.
                        iterator.remove()
                        removedChildProperty = true
                    }
                }
            }

            if (removedChildProperty) {
                // If we removed an entry from this model's vars, we need to ensure hasMore is updated
                var count = 0
                val numVars = codegenProperties.size
                for (codegenProperty in codegenProperties) {
                    count += 1
                    codegenProperty.hasMore = if (count < numVars) true else false
                }
                codegenModel.vars = codegenProperties
            }


            return codegenModel
        }

        @JvmStatic
        fun main(args : Array<String>) {
            SwaggerCodegen.main(args)
        }
    }

}

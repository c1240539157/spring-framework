/*
 * Copyright 2002-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.core.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.core.BridgeMethodResolver;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * Utility class used to collect and merge all annotation attributes on a
 * given {@link AnnotatedElement}, including those declared via meta-annotations.
 *
 * @author Phillip Webb
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 4.0
 */
public class AnnotatedElementUtils {

	public static Set<String> getMetaAnnotationTypes(AnnotatedElement element, String annotationType) {
		final Set<String> types = new LinkedHashSet<String>();
		processWithGetSemantics(element, annotationType, new Processor<Object>() {
			@Override
			public Object process(Annotation annotation, int metaDepth) {
				if (metaDepth > 0) {
					types.add(annotation.annotationType().getName());
				}
				return null;
			}
			@Override
			public void postProcess(Annotation annotation, Object result) {
			}
		});
		return (types.isEmpty() ? null : types);
	}

	public static boolean hasMetaAnnotationTypes(AnnotatedElement element, String annotationType) {
		return Boolean.TRUE.equals(processWithGetSemantics(element, annotationType, new Processor<Boolean>() {
			@Override
			public Boolean process(Annotation annotation, int metaDepth) {
				if (metaDepth > 0) {
					return Boolean.TRUE;
				}
				return null;
			}
			@Override
			public void postProcess(Annotation annotation, Boolean result) {
			}
		}));
	}

	public static boolean isAnnotated(AnnotatedElement element, String annotationType) {
		return Boolean.TRUE.equals(processWithGetSemantics(element, annotationType, new Processor<Boolean>() {
			@Override
			public Boolean process(Annotation annotation, int metaDepth) {
				return Boolean.TRUE;
			}
			@Override
			public void postProcess(Annotation annotation, Boolean result) {
			}
		}));
	}

	/**
	 * <em>Get</em> annotation attributes of the specified {@code annotationType}
	 * in the annotation hierarchy of the supplied {@link AnnotatedElement},
	 * and merge the results into an {@link AnnotationAttributes} map.
	 *
	 * <p>Delegates to {@link #getAnnotationAttributes(AnnotatedElement, String, boolean, boolean)},
	 * supplying {@code false} for {@code classValuesAsString} and {@code nestedAnnotationsAsMap}.
	 *
	 * @param element the annotated element
	 * @param annotationType the annotation type to find
	 * @return the merged {@code AnnotationAttributes}
	 * @see #getAnnotationAttributes(AnnotatedElement, String, boolean, boolean)
	 */
	public static AnnotationAttributes getAnnotationAttributes(AnnotatedElement element, String annotationType) {
		return getAnnotationAttributes(element, annotationType, false, false);
	}

	/**
	 * <em>Get</em> annotation attributes of the specified {@code annotationType}
	 * in the annotation hierarchy of the supplied {@link AnnotatedElement},
	 * and merge the results into an {@link AnnotationAttributes} map.
	 *
	 * @param element the annotated element
	 * @param annotationType the annotation type to find
	 * @param classValuesAsString whether to convert Class references into
	 * Strings or to preserve them as Class references
	 * @param nestedAnnotationsAsMap whether to convert nested Annotation
	 * instances into {@link AnnotationAttributes} maps or to preserve them
	 * as Annotation instances
	 * @return the merged {@code AnnotationAttributes}
	 */
	public static AnnotationAttributes getAnnotationAttributes(AnnotatedElement element, String annotationType,
			boolean classValuesAsString, boolean nestedAnnotationsAsMap) {

		return processWithGetSemantics(element, annotationType, new MergeAnnotationAttributesProcessor(
			classValuesAsString, nestedAnnotationsAsMap));
	}

	/**
	 * <em>Find</em> annotation attributes of the specified {@code annotationType}
	 * in the annotation hierarchy of the supplied {@link AnnotatedElement},
	 * and merge the results into an {@link AnnotationAttributes} map.
	 *
	 * <p>Delegates to
	 * {@link #findAnnotationAttributes(AnnotatedElement, String, boolean, boolean, boolean, boolean, boolean, boolean)},
	 * supplying {@code true} for all {@code search*} flags.
	 *
	 * @param element the annotated element
	 * @param annotationType the annotation type to find
	 * @return the merged {@code AnnotationAttributes}
	 */
	public static AnnotationAttributes findAnnotationAttributes(AnnotatedElement element,
			Class<? extends Annotation> annotationType) {
		return findAnnotationAttributes(element, annotationType.getName(), true, true, true, true, false, false);
	}

	/**
	 * <em>Find</em> annotation attributes of the specified {@code annotationType}
	 * in the annotation hierarchy of the supplied {@link AnnotatedElement},
	 * and merge the results into an {@link AnnotationAttributes} map.
	 *
	 * <p>Delegates to
	 * {@link #findAnnotationAttributes(AnnotatedElement, String, boolean, boolean, boolean, boolean, boolean, boolean)},
	 * supplying {@code true} for all {@code search*} flags.
	 *
	 * @param element the annotated element
	 * @param annotationType the annotation type to find
	 * @return the merged {@code AnnotationAttributes}
	 */
	public static AnnotationAttributes findAnnotationAttributes(AnnotatedElement element, String annotationType) {
		return findAnnotationAttributes(element, annotationType, true, true, true, true, false, false);
	}

	/**
	 * <em>Find</em> annotation attributes of the specified {@code annotationType}
	 * in the annotation hierarchy of the supplied {@link AnnotatedElement},
	 * and merge the results into an {@link AnnotationAttributes} map.
	 *
	 * @param element the annotated element
	 * @param annotationType the annotation type to find
	 * @param searchOnInterfaces whether to search on interfaces, if the
	 * annotated element is a class
	 * @param searchOnSuperclasses whether to search on superclasses, if
	 * the annotated element is a class
	 * @param searchOnMethodsInInterfaces whether to search on methods in
	 * interfaces, if the annotated element is a method
	 * @param searchOnMethodsInSuperclasses whether to search on methods
	 * in superclasses, if the annotated element is a method
	 * @param classValuesAsString whether to convert Class references into
	 * Strings or to preserve them as Class references
	 * @param nestedAnnotationsAsMap whether to convert nested Annotation
	 * instances into {@link AnnotationAttributes} maps or to preserve them
	 * as Annotation instances
	 * @return the merged {@code AnnotationAttributes}
	 */
	public static AnnotationAttributes findAnnotationAttributes(AnnotatedElement element, String annotationType,
			boolean searchOnInterfaces, boolean searchOnSuperclasses, boolean searchOnMethodsInInterfaces,
			boolean searchOnMethodsInSuperclasses, boolean classValuesAsString, boolean nestedAnnotationsAsMap) {

		return processWithFindSemantics(element, annotationType, searchOnInterfaces, searchOnSuperclasses,
			searchOnMethodsInInterfaces, searchOnMethodsInSuperclasses, new MergeAnnotationAttributesProcessor(
				classValuesAsString, nestedAnnotationsAsMap));
	}

	public static MultiValueMap<String, Object> getAllAnnotationAttributes(AnnotatedElement element, String annotationType) {
		return getAllAnnotationAttributes(element, annotationType, false, false);
	}

	public static MultiValueMap<String, Object> getAllAnnotationAttributes(AnnotatedElement element,
			final String annotationType, final boolean classValuesAsString, final boolean nestedAnnotationsAsMap) {

		final MultiValueMap<String, Object> attributes = new LinkedMultiValueMap<String, Object>();
		processWithGetSemantics(element, annotationType, new Processor<Void>() {
			@Override
			public Void process(Annotation annotation, int metaDepth) {
				if (annotation.annotationType().getName().equals(annotationType)) {
					for (Map.Entry<String, Object> entry : AnnotationUtils.getAnnotationAttributes(
							annotation, classValuesAsString, nestedAnnotationsAsMap).entrySet()) {
						attributes.add(entry.getKey(), entry.getValue());
					}
				}
				return null;
			}
			@Override
			public void postProcess(Annotation annotation, Void result) {
				for (String key : attributes.keySet()) {
					if (!AnnotationUtils.VALUE.equals(key)) {
						Object value = AnnotationUtils.getValue(annotation, key);
						if (value != null) {
							attributes.add(key, value);
						}
					}
				}
			}
		});
		return (attributes.isEmpty() ? null : attributes);
	}

	/**
	 * Process all annotations of the specified {@code annotationType} and
	 * recursively all meta-annotations on the specified {@code element}.
	 *
	 * @param element the annotated element
	 * @param annotationType the annotation type to find
	 * @param processor the processor to delegate to
	 * @return the result of the processor
	 */
	private static <T> T processWithGetSemantics(AnnotatedElement element, String annotationType, Processor<T> processor) {
		try {
			return processWithGetSemantics(element, annotationType, processor, new HashSet<AnnotatedElement>(), 0);
		}
		catch (Throwable ex) {
			throw new IllegalStateException("Failed to introspect annotations: " + element, ex);
		}
	}

	/**
	 * Perform the search algorithm for the {@link #processWithGetSemantics}
	 * method, avoiding endless recursion by tracking which annotated elements
	 * have already been <em>visited</em>.
	 *
	 * <p>The {@code metaDepth} parameter represents the depth of the annotation
	 * relative to the initial element. For example, an annotation that is
	 * <em>present</em> on the element will have a depth of 0; a meta-annotation
	 * will have a depth of 1; and a meta-meta-annotation will have a depth of 2.
	 *
	 * @param element the annotated element
	 * @param annotationType the annotation type to find
	 * @param processor the processor to delegate to
	 * @param visited the set of annotated elements that have already been visited
	 * @param metaDepth the depth of the annotation relative to the initial element
	 * @return the result of the processor
	 */
	private static <T> T processWithGetSemantics(AnnotatedElement element, String annotationType,
			Processor<T> processor, Set<AnnotatedElement> visited, int metaDepth) {

		if (visited.add(element)) {
			try {
				// Local annotations: declared OR inherited
				Annotation[] annotations = element.getAnnotations();

				// Search in local annotations
				for (Annotation annotation : annotations) {
					if (annotation.annotationType().getName().equals(annotationType) || metaDepth > 0) {
						T result = processor.process(annotation, metaDepth);
						if (result != null) {
							return result;
						}
						result = processWithGetSemantics(annotation.annotationType(), annotationType, processor,
							visited, metaDepth + 1);
						if (result != null) {
							processor.postProcess(annotation, result);
							return result;
						}
					}
				}

				// Search in meta annotations on local annotations
				for (Annotation annotation : annotations) {
					if (!AnnotationUtils.isInJavaLangAnnotationPackage(annotation)) {
						T result = processWithGetSemantics(annotation.annotationType(), annotationType, processor,
							visited, metaDepth);
						if (result != null) {
							processor.postProcess(annotation, result);
							return result;
						}
					}
				}

			}
			catch (Exception ex) {
				AnnotationUtils.logIntrospectionFailure(element, ex);
			}
		}
		return null;
	}

	/**
	 * Process all annotations of the specified {@code annotationType} and
	 * recursively all meta-annotations on the specified {@code element}.
	 *
	 * @param element the annotated element
	 * @param annotationType the annotation type to find
	 * @param searchOnInterfaces whether to search on interfaces, if the
	 * annotated element is a class
	 * @param searchOnSuperclasses whether to search on superclasses, if
	 * the annotated element is a class
	 * @param searchOnMethodsInInterfaces whether to search on methods in
	 * interfaces, if the annotated element is a method
	 * @param searchOnMethodsInSuperclasses whether to search on methods
	 * in superclasses, if the annotated element is a method
	 * @param processor the processor to delegate to
	 * @return the result of the processor
	 */
	private static <T> T processWithFindSemantics(AnnotatedElement element, String annotationType,
			boolean searchOnInterfaces, boolean searchOnSuperclasses, boolean searchOnMethodsInInterfaces,
			boolean searchOnMethodsInSuperclasses, Processor<T> processor) {

		try {
			return processWithFindSemantics(element, annotationType, searchOnInterfaces, searchOnSuperclasses,
				searchOnMethodsInInterfaces, searchOnMethodsInSuperclasses, processor, new HashSet<AnnotatedElement>(), 0);
		}
		catch (Throwable ex) {
			throw new IllegalStateException("Failed to introspect annotations: " + element, ex);
		}
	}

	/**
	 * Perform the search algorithm for the {@link #process} method, avoiding
	 * endless recursion by tracking which annotated elements have already been
	 * <em>visited</em>.
	 *
	 * <p>The {@code metaDepth} parameter represents the depth of the annotation
	 * relative to the initial element. For example, an annotation that is
	 * <em>present</em> on the element will have a depth of 0; a meta-annotation
	 * will have a depth of 1; and a meta-meta-annotation will have a depth of 2.
	 *
	 * @param element the annotated element
	 * @param annotationType the annotation type to find
	 * @param searchOnInterfaces whether to search on interfaces, if the
	 * annotated element is a class
	 * @param searchOnSuperclasses whether to search on superclasses, if
	 * the annotated element is a class
	 * @param searchOnMethodsInInterfaces whether to search on methods in
	 * interfaces, if the annotated element is a method
	 * @param searchOnMethodsInSuperclasses whether to search on methods
	 * in superclasses, if the annotated element is a method
	 * @param processor the processor to delegate to
	 * @param visited the set of annotated elements that have already been visited
	 * @param metaDepth the depth of the annotation relative to the initial element
	 * @return the result of the processor
	 */
	private static <T> T processWithFindSemantics(AnnotatedElement element, String annotationType,
			boolean searchOnInterfaces, boolean searchOnSuperclasses, boolean searchOnMethodsInInterfaces,
			boolean searchOnMethodsInSuperclasses, Processor<T> processor, Set<AnnotatedElement> visited, int metaDepth) {

		if (visited.add(element)) {
			try {

				// Local annotations: declared or (declared + inherited).
				Annotation[] annotations = (searchOnSuperclasses ? element.getDeclaredAnnotations()
						: element.getAnnotations());

				// Search in local annotations
				for (Annotation annotation : annotations) {
					if (annotation.annotationType().getName().equals(annotationType) || metaDepth > 0) {
						T result = processor.process(annotation, metaDepth);
						if (result != null) {
							return result;
						}
						result = processWithFindSemantics(annotation.annotationType(), annotationType,
							searchOnInterfaces, searchOnSuperclasses, searchOnMethodsInInterfaces,
							searchOnMethodsInSuperclasses, processor, visited, metaDepth + 1);
						if (result != null) {
							processor.postProcess(annotation, result);
							return result;
						}
					}
				}

				// Search in meta annotations on local annotations
				for (Annotation annotation : annotations) {
					if (!AnnotationUtils.isInJavaLangAnnotationPackage(annotation)) {
						T result = processWithFindSemantics(annotation.annotationType(), annotationType,
							searchOnInterfaces, searchOnSuperclasses, searchOnMethodsInInterfaces,
							searchOnMethodsInSuperclasses, processor, visited, metaDepth);
						if (result != null) {
							processor.postProcess(annotation, result);
							return result;
						}
					}
				}

				if (element instanceof Method) {
					Method method = (Method) element;

					// Search on possibly bridged method
					Method resolvedMethod = BridgeMethodResolver.findBridgedMethod(method);
					T result = processWithFindSemantics(resolvedMethod, annotationType, searchOnInterfaces,
						searchOnSuperclasses, searchOnMethodsInInterfaces, searchOnMethodsInSuperclasses, processor,
						visited, metaDepth);
					if (result != null) {
						return result;
					}

					// Search on methods in interfaces declared locally
					if (searchOnMethodsInInterfaces) {
						Class<?>[] ifcs = method.getDeclaringClass().getInterfaces();
						result = searchOnInterfaces(method, annotationType, searchOnInterfaces, searchOnSuperclasses,
							searchOnMethodsInInterfaces, searchOnMethodsInSuperclasses, processor, visited, metaDepth,
							ifcs);
						if (result != null) {
							return result;
						}
					}

					// Search on methods in class hierarchy and interface hierarchy
					if (searchOnMethodsInSuperclasses) {
						Class<?> clazz = method.getDeclaringClass();
						while (true) {
							clazz = clazz.getSuperclass();
							if (clazz == null || clazz.equals(Object.class)) {
								break;
							}

							try {
								// TODO [SPR-12738] Resolve equivalent parameterized
								// method (i.e., bridged method) in superclass.
								Method equivalentMethod = clazz.getDeclaredMethod(method.getName(),
									method.getParameterTypes());
								Method resolvedEquivalentMethod = BridgeMethodResolver.findBridgedMethod(equivalentMethod);
								result = processWithFindSemantics(resolvedEquivalentMethod, annotationType,
									searchOnInterfaces, searchOnSuperclasses, searchOnMethodsInInterfaces,
									searchOnMethodsInSuperclasses, processor, visited, metaDepth);
								if (result != null) {
									return result;
								}
							}
							catch (NoSuchMethodException ex) {
								// No equivalent method found
							}

							// Search on interfaces declared on superclass
							if (searchOnMethodsInInterfaces) {
								result = searchOnInterfaces(method, annotationType, searchOnInterfaces,
									searchOnSuperclasses, searchOnMethodsInInterfaces, searchOnMethodsInSuperclasses,
									processor, visited, metaDepth, clazz.getInterfaces());
								if (result != null) {
									return result;
								}
							}
						}
					}
				}

				if (element instanceof Class) {
					Class<?> clazz = (Class<?>) element;

					// Search on interfaces
					if (searchOnInterfaces) {
						for (Class<?> ifc : clazz.getInterfaces()) {
							T result = processWithFindSemantics(ifc, annotationType, searchOnInterfaces,
								searchOnSuperclasses, searchOnMethodsInInterfaces, searchOnMethodsInSuperclasses,
								processor, visited, metaDepth);
							if (result != null) {
								return result;
							}
						}
					}

					// Search on superclass
					if (searchOnSuperclasses) {
						Class<?> superclass = clazz.getSuperclass();
						if (superclass != null && !superclass.equals(Object.class)) {
							T result = processWithFindSemantics(superclass, annotationType, searchOnInterfaces,
								searchOnSuperclasses, searchOnMethodsInInterfaces, searchOnMethodsInSuperclasses,
								processor, visited, metaDepth);
							if (result != null) {
								return result;
							}
						}
					}
				}
			}
			catch (Exception ex) {
				AnnotationUtils.logIntrospectionFailure(element, ex);
			}
		}
		return null;
	}

	private static <T> T searchOnInterfaces(Method method, String annotationType, boolean searchOnInterfaces,
			boolean searchOnSuperclasses, boolean searchOnMethodsInInterfaces, boolean searchOnMethodsInSuperclasses,
			Processor<T> processor, Set<AnnotatedElement> visited, int metaDepth, Class<?>[] ifcs) {

		for (Class<?> iface : ifcs) {
			if (AnnotationUtils.isInterfaceWithAnnotatedMethods(iface)) {
				try {
					Method equivalentMethod = iface.getMethod(method.getName(), method.getParameterTypes());
					T result = processWithFindSemantics(equivalentMethod, annotationType, searchOnInterfaces,
						searchOnSuperclasses, searchOnMethodsInInterfaces, searchOnMethodsInSuperclasses, processor,
						visited, metaDepth);

					if (result != null) {
						return result;
					}
				}
				catch (NoSuchMethodException ex) {
					// Skip this interface - it doesn't have the method...
				}
			}
		}

		return null;
	}


	/**
	 * Callback interface that is used to process a target annotation that
	 * was found as the result of a search and to post-process the result as
	 * the search algorithm goes back down the annotation hierarchy from
	 * the target annotation to the initial {@link AnnotatedElement}.
	 *
	 * @param <T> the result type
	 */
	private static interface Processor<T> {

		/**
		 * Process the actual target annotation once it has been found by
		 * the search algorithm.
		 *
		 * <p>The {@code metaDepth} parameter represents the depth of the
		 * annotation relative to the initial element. For example, an annotation
		 * that is <em>present</em> on the element will have a depth of 0; a
		 * meta-annotation will have a depth of 1; and a meta-meta-annotation
		 * will have a depth of 2.
		 *
		 * @param annotation the annotation to process
		 * @param metaDepth the depth of the annotation relative to the initial element
		 * @return the result of the processing, or {@code null} to continue
		 */
		T process(Annotation annotation, int metaDepth);

		/**
		 * Post-process the result returned by the {@link #process} method.
		 *
		 * <p>The {@code annotation} supplied to this method is an annotation
		 * that is present in the annotation hierarchy, above the initial
		 * {@link AnnotatedElement} but below the target annotation found by
		 * the search algorithm.
		 *
		 * @param annotation the annotation to post-process
		 * @param result the result to post-process
		 */
		void postProcess(Annotation annotation, T result);
	}

	private static class MergeAnnotationAttributesProcessor implements Processor<AnnotationAttributes> {

		private final boolean classValuesAsString;
		private final boolean nestedAnnotationsAsMap;


		MergeAnnotationAttributesProcessor(boolean classValuesAsString, boolean nestedAnnotationsAsMap) {
			this.classValuesAsString = classValuesAsString;
			this.nestedAnnotationsAsMap = nestedAnnotationsAsMap;
		}

		@Override
		public AnnotationAttributes process(Annotation annotation, int metaDepth) {
			return AnnotationUtils.getAnnotationAttributes(annotation, classValuesAsString, nestedAnnotationsAsMap);
		}

		@Override
		public void postProcess(Annotation annotation, AnnotationAttributes result) {
			for (String key : result.keySet()) {
				if (!AnnotationUtils.VALUE.equals(key)) {
					Object value = AnnotationUtils.getValue(annotation, key);
					if (value != null) {
						result.put(key, AnnotationUtils.adaptValue(value, classValuesAsString, nestedAnnotationsAsMap));
					}
				}
			}
		}
	}

}

// @ts-check
import { readFileSync } from 'fs';
import { defineConfig, envField } from 'astro/config';
import starlight from '@astrojs/starlight';
import starlightLinksValidator from 'starlight-links-validator';

let retrofitProperties = readFileSync('../gradle.properties');
let retrofitVersion = /VERSION_NAME=(.*?)\n/.exec(retrofitProperties)[1];

export default defineConfig({
	site: 'https://square.github.io',
	base: '/retrofit/latest',
	env: {
		schema: {
			VERSION: envField.string({ context: 'server', access: 'public', optional: true, default: retrofitVersion }),
		},
	},
	integrations: [
		starlight({
			title: 'Retrofit',
			customCss: [
				'./src/styles/theme.css',
			],
			editLink: {
				baseUrl: 'https://github.com/square/retrofit/edit/trunk/website',
			},
			social: [
				{ icon: 'stackOverflow', label: 'StackOverflow', href: 'https://stackoverflow.com/questions/tagged/retrofit?sort=active' },
				{ icon: 'github', label: 'GitHub', href: 'https://github.com/square/retrofit' },
			],
			sidebar: [
				{
					label: 'Documentation',
					items: [
						{ slug: 'index' },
						{ slug: 'declarations' },
						{ slug: 'configuration' },
						{ slug: 'download' },
						{ slug: 'contributing' },
					]
				},
				{
					label: 'Resources',
					items: [
						{ label: 'GitHub', link: 'https://github.com/square/retrofit' },
						{
							label: 'Javadoc',
							collapsed: true,
							items: [
								{ label: 'retrofit', link: '/3.x/retrofit/' },
								{ label: 'retrofit-mock', link: '/3.x/retrofit-mock/' },
								{ label: 'converter-gson', link: '/3.x/converter-gson/' },
								{ label: 'converter-guava', link: '/3.x/converter-guava/' },
								{ label: 'converter-jackson', link: '/3.x/converter-jackson/' },
								{ label: 'converter-java8', link: '/3.x/converter-java8/' },
								{ label: 'converter-jaxb', link: '/3.x/converter-jaxb/' },
								{ label: 'converter-jaxb3', link: '/3.x/converter-jaxb3/' },
								{ label: 'converter-kotlinx-serialization', link: '/3.x/converter-kotlinx-serialization/' },
								{ label: 'converter-moshi', link: '/3.x/converter-moshi/' },
								{ label: 'converter-protobuf', link: '/3.x/converter-protobuf/' },
								{ label: 'converter-scalars', link: '/3.x/converter-scalars/' },
								{ label: 'converter-simplexml', link: '/3.x/converter-simplexml/' },
								{ label: 'converter-wire', link: '/3.x/converter-wire/' },
								{ label: 'adapter-guava', link: '/3.x/adapter-guava/' },
								{ label: 'adapter-java8', link: '/3.x/adapter-java8/' },
								{ label: 'adapter-rxjava', link: '/3.x/adapter-rxjava/' },
								{ label: 'adapter-rxjava2', link: '/3.x/adapter-rxjava2/' },
								{ label: 'adapter-rxjava3', link: '/3.x/adapter-rxjava3/' },
								{ label: 'adapter-scala', link: '/3.x/adapter-scala/' },
							],
						},
						{
							label: 'Javadoc (2.x)',
							collapsed: true,
							items: [
								{ label: 'retrofit', link: '/2.x/retrofit/' },
								{ label: 'retrofit-mock', link: '/2.x/retrofit-mock/' },
								{ label: 'converter-gson', link: '/2.x/converter-gson/' },
								{ label: 'converter-guava', link: '/2.x/converter-guava/' },
								{ label: 'converter-jackson', link: '/2.x/converter-jackson/' },
								{ label: 'converter-java8', link: '/2.x/converter-java8/' },
								{ label: 'converter-jaxb', link: '/2.x/converter-jaxb/' },
								{ label: 'converter-jaxb3', link: '/2.x/converter-jaxb3/' },
								{ label: 'converter-kotlinx-serialization', link: '/2.x/converter-kotlinx-serialization/' },
								{ label: 'converter-moshi', link: '/2.x/converter-moshi/' },
								{ label: 'converter-protobuf', link: '/2.x/converter-protobuf/' },
								{ label: 'converter-scalars', link: '/2.x/converter-scalars/' },
								{ label: 'converter-simplexml', link: '/2.x/converter-simplexml/' },
								{ label: 'converter-wire', link: '/2.x/converter-wire/' },
								{ label: 'adapter-guava', link: '/2.x/adapter-guava/' },
								{ label: 'adapter-java8', link: '/2.x/adapter-java8/' },
								{ label: 'adapter-rxjava', link: '/2.x/adapter-rxjava/' },
								{ label: 'adapter-rxjava2', link: '/2.x/adapter-rxjava2/' },
								{ label: 'adapter-rxjava3', link: '/2.x/adapter-rxjava3/' },
								{ label: 'adapter-scala', link: '/2.x/adapter-scala/' },
							],
						},
						{
							label: 'Javadoc (1.x)',
							collapsed: true,
							items: [
								{ label: 'retrofit', link: '/1.x/retrofit/' },
								{ label: 'retrofit-mock', link: '/1.x/retrofit-mock/' },
								{ label: 'converter-jackson', link: '/1.x/converter-jackson/' },
								{ label: 'converter-protobuf', link: '/1.x/converter-protobuf/' },
								{ label: 'converter-simplexml', link: '/1.x/converter-simplexml/' },
								{ label: 'converter-wire', link: '/1.x/converter-wire/' },
							],
						},
						{ label: 'StackOverflow', link: 'https://stackoverflow.com/questions/tagged/retrofit?sort=active' },
					],
				}
			],
			plugins: [
				starlightLinksValidator(),
			],
		}),
	],
});

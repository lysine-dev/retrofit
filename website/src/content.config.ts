import { defineCollection, z } from 'astro:content';
import { docsLoader } from '@astrojs/starlight/loaders';
import { docsSchema } from '@astrojs/starlight/schema';

export const collections = {
	docs: defineCollection({
		loader: docsLoader(),
		schema: docsSchema({
			// In non-"release" mode, add a banner indicating snapshot documentation.
			extend: z.object(import.meta.env.MODE == "release" && {} || {
				banner: z.object({ content: z.string() }).default({
					content: `You are viewing the snapshot documentation. Looking for the <a href="https://square.github.io/retrofit/">release documentation</a>?`,
				}),
			}),
		}),
	}),
};
